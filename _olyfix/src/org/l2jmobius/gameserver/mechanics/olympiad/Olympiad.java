package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class Olympiad extends ListenersContainer {
   protected static final Logger LOGGER = Logger.getLogger(Olympiad.class.getName());
   protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
   private static final Map<Integer, StatSet> NOBLES = new ConcurrentHashMap<>();
   protected static List<StatSet> HEROS_TO_BE;
   private static Map<Integer, Integer> NOBLES_RANK = new HashMap<>();
   private static List<Player> _nonClassBasedRegisters;
   private static Map<Integer, List<Player>> _classBasedRegisters;
   public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
   /**
    * Pre-battle-start hooks (scripts, e.g. FakePlayersAI). Each consumer is invoked for BOTH match
    * participants exactly 2 seconds before the battle starts (during the arena countdown, at the
    * "The match will start in 2 seconds" mark). Exceptions thrown by a hook are caught and logged.
    */
   public static final List<Consumer<Player>> PRE_START_HOOKS = new CopyOnWriteArrayList<>();
   private static final String OLYMPIAD_LOAD_DATA = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0";
   private static final String OLYMPIAD_SAVE_DATA = "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?";
   private static final String OLYMPIAD_LOAD_NOBLES = "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
   private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles (`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,`competitions_drawn`) VALUES (?,?,?,?,?,?,?)";
   private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?";
   private static final String OLYMPIAD_GET_HEROS = "SELECT olympiad_nobles.charId, characters.char_name FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC";
   private static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT charId from olympiad_nobles_eom WHERE competitions_done >= 9 ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
   private static final String GET_EACH_CLASS_LEADER = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
   private static final String GET_EACH_CLASS_LEADER_CURRENT = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
   private static final String GET_EACH_CLASS_LEADER_SOULHOUND = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = 133) AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
   private static final String GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = 133) AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
   private static final String REMOVE_UNCLAIMED_POINTS = "DELETE FROM character_variables WHERE charId=? AND var=?";
   private static final String INSERT_UNCLAIMED_POINTS = "INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)";
   public static final String UNCLAIMED_OLYMPIAD_PASSES_VAR = "UNCLAIMED_OLYMPIAD_POINTS";
   private static final String OLYMPIAD_DELETE_ALL = "TRUNCATE olympiad_nobles";
   private static final String OLYMPIAD_MONTH_CLEAR = "TRUNCATE olympiad_nobles_eom";
   private static final String OLYMPIAD_MONTH_CREATE = "INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles";
   private static final int[] HERO_IDS = new int[]{
      88,
      89,
      90,
      91,
      92,
      93,
      94,
      95,
      96,
      97,
      98,
      99,
      100,
      101,
      102,
      103,
      104,
      105,
      106,
      107,
      108,
      109,
      110,
      111,
      112,
      113,
      114,
      115,
      116,
      117,
      118,
      131,
      132,
      133,
      134
   };
   private static final int COMP_START = OlympiadConfig.OLYMPIAD_START_TIME;
   private static final int COMP_MIN = OlympiadConfig.OLYMPIAD_MIN;
   private static final long COMP_PERIOD = OlympiadConfig.OLYMPIAD_CPERIOD;
   protected static final long WEEKLY_PERIOD = OlympiadConfig.OLYMPIAD_WPERIOD;
   protected static final long VALIDATION_PERIOD = OlympiadConfig.OLYMPIAD_VPERIOD;
   protected static final int DEFAULT_POINTS = OlympiadConfig.OLYMPIAD_START_POINTS;
   protected static final int WEEKLY_POINTS = OlympiadConfig.OLYMPIAD_WEEKLY_POINTS;
   public static final String CHAR_ID = "charId";
   public static final String CLASS_ID = "class_id";
   public static final String CHAR_NAME = "char_name";
   public static final String POINTS = "olympiad_points";
   public static final String COMP_DONE = "competitions_done";
   public static final String COMP_WON = "competitions_won";
   public static final String COMP_LOST = "competitions_lost";
   public static final String COMP_DRAWN = "competitions_drawn";
   protected long _olympiadEnd;
   protected long _validationEnd;
   protected int _period;
   protected long _nextWeeklyChange;
   protected int _currentCycle;
   private long _compEnd;
   private Calendar _compStart;
   protected static boolean _inCompPeriod;
   protected static boolean _compStarted = false;
   protected ScheduledFuture<?> _scheduledCompStart;
   protected ScheduledFuture<?> _scheduledCompEnd;
   protected ScheduledFuture<?> _scheduledOlympiadEnd;
   protected ScheduledFuture<?> _scheduledWeeklyTask;
   protected ScheduledFuture<?> _scheduledValdationTask;

   Olympiad() {
      if (OlympiadConfig.OLYMPIAD_ENABLED) {
         this.load();
         AntiFeedManager.getInstance().registerEvent(1);
         if (this._period == 0) {
            this.init();
         }
      } else {
         LOGGER.log(Level.INFO, "Disabled.");
      }
   }

   public static Integer getStadiumCount() {
      return OlympiadManager.STADIUMS.length;
   }

   private void load() {
      NOBLES.clear();
      boolean loaded = false;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0"
         );
         ResultSet rset = statement.executeQuery();
      ) {
         while (rset.next()) {
            this._currentCycle = rset.getInt("current_cycle");
            this._period = rset.getInt("period");
            this._olympiadEnd = rset.getLong("olympiad_end");
            this._validationEnd = rset.getLong("validation_end");
            this._nextWeeklyChange = rset.getLong("next_weekly_change");
            loaded = true;
         }
      } catch (Exception var33) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading olympiad data from database: ", (Throwable)var33);
      }

      if (!loaded) {
         this._currentCycle = 1;
         this._period = 0;
         this._olympiadEnd = 0L;
         this._validationEnd = 0L;
         this._nextWeeklyChange = 0L;
      }

      long currentTime = System.currentTimeMillis();
      switch (this._period) {
         case 0:
            if (this._olympiadEnd != 0L && this._olympiadEnd >= currentTime) {
               this.scheduleWeeklyChange();
            } else {
               this.setNewOlympiadEnd();
            }
            break;
         case 1:
            if (this._validationEnd > currentTime) {
               this.loadNoblesRank();
               this._scheduledValdationTask = ThreadPool.schedule(new Olympiad.ValidationEndTask(), this.getMillisToValidationEnd());
            } else {
               this._currentCycle++;
               this._period = 0;
               this.deleteNobles();
               this.setNewOlympiadEnd();
            }
            break;
         default:
            LOGGER.warning("Olympiad System: Omg something went wrong in loading!! Period = " + this._period);
            return;
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId"
         );
         ResultSet rset = statement.executeQuery();
      ) {
         while (rset.next()) {
            StatSet statData = new StatSet();
            int charId = rset.getInt("charId");
            statData.set("class_id", rset.getInt("class_id"));
            statData.set("char_name", rset.getString("char_name"));
            int compDone = rset.getInt("competitions_done");
            statData.set(
               "olympiad_points",
               Math.clamp((long)rset.getInt("olympiad_points"), 0, OlympiadConfig.OLYMPIAD_MAX_POINTS * compDone + OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4)
            );
            statData.set("competitions_done", compDone);
            statData.set("competitions_won", rset.getInt("competitions_won"));
            statData.set("competitions_lost", rset.getInt("competitions_lost"));
            statData.set("competitions_drawn", rset.getInt("competitions_drawn"));
            statData.set("to_save", false);
            NOBLES.put(charId, statData);
         }

         rset.close();
         statement.close();
      } catch (Exception var29) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database: ", (Throwable)var29);
      }

      LOGGER.info("Olympiad System: Loading Olympiad System....");
      if (this._period == 0) {
         LOGGER.info("Olympiad System: Currently in Olympiad Period");
      } else {
         LOGGER.info("Olympiad System: Currently in Validation Period");
      }

      long milliToEnd;
      if (this._period == 0) {
         milliToEnd = this.getMillisToOlympiadEnd();
      } else {
         milliToEnd = this.getMillisToValidationEnd();
      }

      double numSecs = milliToEnd / 1000L % 60L;
      double countDown = (milliToEnd / 1000.0 - numSecs) / 60.0;
      int numMins = (int)(countDown % 60.0);
      countDown = (countDown - numMins) / 60.0;
      int numHours = (int)(countDown % 24.0);
      int numDays = (int)((countDown - numHours) / 24.0);
      LOGGER.info("Olympiad System: " + numDays + " days, " + numHours + " hours and " + numMins + " mins until period ends.");
      if (this._period == 0) {
         milliToEnd = this.getMillisToWeekChange();
         double numSecs2 = milliToEnd / 1000L % 60L;
         double countDown2 = (milliToEnd / 1000.0 - numSecs2) / 60.0;
         int numMins2 = (int)(countDown % 60.0);
         countDown2 = (countDown2 - numMins) / 60.0;
         int numHours2 = (int)(countDown2 % 24.0);
         int numDays2 = (int)((countDown2 - numHours) / 24.0);
         LOGGER.info("Olympiad System: Next weekly change is in " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
      }

      LOGGER.info("Olympiad System: Loaded " + NOBLES.size() + " Nobles");
   }

   public void loadNoblesRank() {
      NOBLES_RANK.clear();
      Map<Integer, Integer> tmpPlace = new HashMap<>();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "SELECT charId from olympiad_nobles_eom WHERE competitions_done >= 9 ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC"
         );
         ResultSet rset = statement.executeQuery();
      ) {
         int place = 1;

         while (rset.next()) {
            tmpPlace.put(rset.getInt("charId"), place++);
         }
      } catch (Exception var30) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database for Ranking: ", (Throwable)var30);
      }

      int rank1 = (int)Math.round(tmpPlace.size() * 0.01);
      int rank2 = (int)Math.round(tmpPlace.size() * 0.1);
      int rank3 = (int)Math.round(tmpPlace.size() * 0.25);
      int rank4 = (int)Math.round(tmpPlace.size() * 0.5);
      if (rank1 == 0) {
         rank1 = 1;
         rank2++;
         rank3++;
         rank4++;
      }

      for (Entry<Integer, Integer> chr : tmpPlace.entrySet()) {
         if (chr.getValue() <= rank1) {
            NOBLES_RANK.put(chr.getKey(), 1);
         } else if (tmpPlace.get(chr.getKey()) <= rank2) {
            NOBLES_RANK.put(chr.getKey(), 2);
         } else if (tmpPlace.get(chr.getKey()) <= rank3) {
            NOBLES_RANK.put(chr.getKey(), 3);
         } else if (tmpPlace.get(chr.getKey()) <= rank4) {
            NOBLES_RANK.put(chr.getKey(), 4);
         } else {
            NOBLES_RANK.put(chr.getKey(), 5);
         }
      }

      for (int noblesId : NOBLES.keySet()) {
         int points = this.getNoblessePasses(noblesId);
         if (points > 0) {
            Player player = World.getPlayer(noblesId);
            if (player != null) {
               player.getVariables().set("UNCLAIMED_OLYMPIAD_POINTS", points);
            } else {
               try (
                  Connection con = DatabaseFactory.getConnection();
                  PreparedStatement statement = con.prepareStatement("DELETE FROM character_variables WHERE charId=? AND var=?");
               ) {
                  statement.setInt(1, noblesId);
                  statement.setString(2, "UNCLAIMED_OLYMPIAD_POINTS");
                  statement.execute();
               } catch (SQLException var26) {
                  LOGGER.warning("Olympiad System: Couldn't remove unclaimed olympiad points from DB!");
               }

               try (
                  Connection con = DatabaseFactory.getConnection();
                  PreparedStatement statement = con.prepareStatement("INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)");
               ) {
                  statement.setInt(1, noblesId);
                  statement.setString(2, "UNCLAIMED_OLYMPIAD_POINTS");
                  statement.setString(3, String.valueOf(points));
                  statement.execute();
               } catch (SQLException var23) {
                  LOGGER.warning("Olympiad System: Couldn't store unclaimed olympiad points to DB!");
               }
            }
         }
      }
   }

   protected void init() {
      if (this._period != 1) {
         _nonClassBasedRegisters = new ArrayList<>();
         _classBasedRegisters = new HashMap<>();
         this._compStart = Calendar.getInstance();
         if (OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS) {
            int currentDay = this._compStart.get(7);
            boolean dayFound = false;
            int dayCounter = 0;

            for (int i = currentDay; i < 8; i++) {
               if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i)) {
                  dayFound = true;
                  break;
               }

               dayCounter++;
            }

            if (!dayFound) {
               for (int i = 1; i < 8 && !OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i); i++) {
                  dayCounter++;
               }
            }

            if (dayCounter > 0) {
               this._compStart.add(5, dayCounter);
            }
         }

         this._compStart.set(11, COMP_START);
         this._compStart.set(12, COMP_MIN);
         this._compEnd = this._compStart.getTimeInMillis() + COMP_PERIOD;
         if (this._scheduledOlympiadEnd != null) {
            this._scheduledOlympiadEnd.cancel(true);
         }

         this._scheduledOlympiadEnd = ThreadPool.schedule(new Olympiad.OlympiadEndTask(), this.getMillisToOlympiadEnd());
         this.updateCompStatus();
      }
   }

   public boolean registerNoble(Player noble, boolean classBased) {
      if (!_inCompPeriod) {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
         noble.sendPacket(sm);
         return false;
      } else if (!noble.isNoble()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
         sm.addPcName(noble);
         noble.sendPacket(sm);
         return false;
      } else if (noble.getBaseClass() != noble.getPlayerClass().getId()) {
         SystemMessage sm = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_A_SUB_CLASS_CHARACTER_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD
         );
         sm.addPcName(noble);
         noble.sendPacket(sm);
         return false;
      } else if (noble.isCursedWeaponEquipped()) {
         SystemMessage sm = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD
         );
         sm.addPcName(noble);
         sm.addItemName(noble.getCursedWeaponEquippedId());
         noble.sendPacket(sm);
         return false;
      } else if (noble.getInventoryLimit() * 0.8 <= noble.getInventory().getSize()) {
         SystemMessage sm = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80
         );
         sm.addPcName(noble);
         noble.sendPacket(sm);
         return false;
      } else if (this.getMillisToCompEnd() < 600000L) {
         SystemMessage sm = new SystemMessage(SystemMessageId.PARTICIPATION_REQUESTS_ARE_NO_LONGER_BEING_ACCEPTED);
         noble.sendPacket(sm);
         return false;
      } else if (noble.isRegisteredOnEvent()) {
         noble.sendMessage("You can't join olympiad while participating on events.");
         return false;
      } else {
         if (_classBasedRegisters.containsKey(noble.getPlayerClass().getId())) {
            for (Player participant : _classBasedRegisters.get(noble.getPlayerClass().getId())) {
               if (participant.getObjectId() == noble.getObjectId()) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
                  sm.addPcName(noble);
                  noble.sendPacket(sm);
                  return false;
               }
            }
         }

         if (this.isRegisteredInComp(noble)) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_CLASS_IRRELEVANT_INDIVIDUAL_MATCH);
            sm.addPcName(noble);
            noble.sendPacket(sm);
            return false;
         } else {
            if (!NOBLES.containsKey(noble.getObjectId())) {
               StatSet statDat = new StatSet();
               statDat.set("class_id", noble.getPlayerClass().getId());
               statDat.set("char_name", noble.getName());
               statDat.set("olympiad_points", DEFAULT_POINTS);
               statDat.set("competitions_done", 0);
               statDat.set("competitions_won", 0);
               statDat.set("competitions_lost", 0);
               statDat.set("competitions_drawn", 0);
               statDat.set("to_save", true);
               NOBLES.put(noble.getObjectId(), statDat);
            }

            if (classBased && this.getNoblePoints(noble.getObjectId()) < 3) {
               noble.sendMessage("Cannot register when you have less than 3 points");
               return false;
            } else if (!classBased && this.getNoblePoints(noble.getObjectId()) < 5) {
               noble.sendMessage("Cannot register when you have less than 5 points");
               return false;
            } else {
               if (classBased) {
                  if (_classBasedRegisters.containsKey(noble.getPlayerClass().getId())) {
                     List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
                     classed.add(noble);
                     _classBasedRegisters.remove(noble.getPlayerClass().getId());
                     _classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
                  } else {
                     List<Player> classed = new ArrayList<>();
                     classed.add(noble);
                     _classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
                  }

                  SystemMessage sm = new SystemMessage(
                     SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST_FOR_A_CLASS_SPECIFIC_MATCH
                  );
                  noble.sendPacket(sm);
               } else {
                  _nonClassBasedRegisters.add(noble);
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOU_ARE_CURRENTLY_REGISTERED_FOR_A_1V1_CLASS_IRRELEVANT_MATCH);
                  noble.sendPacket(sm);
               }

               return true;
            }
         }
      }
   }

   protected static int getNobleCount() {
      return NOBLES.size();
   }

   protected static StatSet getNobleStats(int playerId) {
      return NOBLES.get(playerId);
   }

   protected static List<Player> getRegisteredNonClassBased() {
      return _nonClassBasedRegisters;
   }

   protected static Map<Integer, List<Player>> getRegisteredClassBased() {
      return _classBasedRegisters;
   }

   protected static List<Integer> hasEnoughRegisteredClassed() {
      List<Integer> result = new ArrayList<>();

      for (Integer classList : getRegisteredClassBased().keySet()) {
         if (getRegisteredClassBased().get(classList).size() >= OlympiadConfig.OLYMPIAD_CLASSED) {
            result.add(classList);
         }
      }

      return !result.isEmpty() ? result : null;
   }

   protected static boolean hasEnoughRegisteredNonClassed() {
      return getRegisteredNonClassBased().size() >= OlympiadConfig.OLYMPIAD_NONCLASSED;
   }

   protected static void clearRegistered() {
      _nonClassBasedRegisters.clear();
      _classBasedRegisters.clear();
   }

   public boolean isRegistered(Player noble) {
      boolean result = false;
      if (_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(noble)) {
         result = true;
      } else if (_classBasedRegisters != null && _classBasedRegisters.containsKey(noble.getPlayerClass().getId())) {
         List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
         if (classed != null && classed.contains(noble)) {
            result = true;
         }
      }

      return result;
   }

   public boolean unRegisterNoble(Player noble) {
      if (!_inCompPeriod) {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
         noble.sendPacket(sm);
         return false;
      } else if (!noble.isNoble()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
         sm.addString(noble.getName());
         noble.sendPacket(sm);
         return false;
      } else if (!this.isRegistered(noble)) {
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_ARE_NOT_CURRENTLY_REGISTERED_ON_ANY_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
         noble.sendPacket(sm);
         return false;
      } else {
         for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values()) {
            if (game != null && (game._playerOneID == noble.getObjectId() || game._playerTwoID == noble.getObjectId())) {
               noble.sendMessage("Can't deregister whilst you are already selected for a game");
               return false;
            }
         }

         if (_nonClassBasedRegisters.contains(noble)) {
            _nonClassBasedRegisters.remove(noble);
         } else {
            List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
            classed.remove(noble);
            _classBasedRegisters.remove(noble.getPlayerClass().getId());
            _classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
         }

         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REMOVED_FROM_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
         noble.sendPacket(sm);
         if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) {
            AntiFeedManager.getInstance().removePlayer(1, noble);
         }

         return true;
      }
   }

   public void removeDisconnectedCompetitor(Player player) {
      if (OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()) != null) {
         OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()).handleDisconnect(player);
      }

      List<Player> classed = _classBasedRegisters.get(player.getPlayerClass().getId());
      if (_nonClassBasedRegisters.contains(player)) {
         _nonClassBasedRegisters.remove(player);
      } else if (classed != null && classed.contains(player)) {
         classed.remove(player);
         _classBasedRegisters.remove(player.getPlayerClass().getId());
         _classBasedRegisters.put(player.getPlayerClass().getId(), classed);
      }
   }

   public void notifyCompetitorDamage(Player player, int damage, int gameId) {
      if (OlympiadManager.getInstance().getOlympiadGames().get(gameId) != null) {
         OlympiadManager.getInstance().getOlympiadGames().get(gameId).addDamage(player, damage);
      }
   }

   private void updateCompStatus() {
      long milliToStart = this.getMillisToCompBegin();
      double numSecs = milliToStart / 1000L % 60L;
      double countDown = (milliToStart / 1000L - numSecs) / 60.0;
      int numMins = (int)(countDown % 60.0);
      countDown = (countDown - numMins) / 60.0;
      int numHours = (int)(countDown % 24.0);
      int numDays = (int)((countDown - numHours) / 24.0);
      LOGGER.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
      LOGGER.info("Olympiad System: Event starts/started : " + this._compStart.getTime());
      this._scheduledCompStart = ThreadPool.schedule(
         () -> {
            if (!this.isOlympiadEnd()) {
               _inCompPeriod = true;
               OlympiadManager om = OlympiadManager.getInstance();
               World.broadcastToAllOnlinePlayers(
                  new SystemMessage(
                     SystemMessageId.SHARPEN_YOUR_SWORDS_TIGHTEN_THE_STITCHINGS_IN_YOUR_ARMOR_AND_MAKE_HASTE_TO_A_GRAND_OLYMPIAD_MANAGER_BATTLES_IN_THE_GRAND_OLYMPIAD_GAMES_ARE_NOW_TAKING_PLACE
                  )
               );
               LOGGER.info("Olympiad System: Olympiad Game Started");
               LOGGER_OLYMPIAD.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
               Thread olyCycle = new Thread(om);
               olyCycle.start();
               long regEnd = this.getMillisToCompEnd() - 600000L;
               if (regEnd > 0L) {
                  ThreadPool.schedule(
                     () -> World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_REGISTRATION_PERIOD_HAS_ENDED)), regEnd
                  );
               }

               this._scheduledCompEnd = ThreadPool.schedule(
                  () -> {
                     if (!this.isOlympiadEnd()) {
                        _inCompPeriod = false;
                        World.broadcastToAllOnlinePlayers(
                           new SystemMessage(
                              SystemMessageId.MUCH_CARNAGE_HAS_BEEN_LEFT_FOR_THE_CLEANUP_CREW_OF_THE_OLYMPIAD_STADIUM_BATTLES_IN_THE_GRAND_OLYMPIAD_GAMES_ARE_NOW_OVER
                           )
                        );
                        LOGGER.info("Olympiad System: Olympiad Game Ended");

                        while (OlympiadGame._battleStarted) {
                           try {
                              Thread.sleep(60000L);
                           } catch (Exception var2x) {
                           }
                        }

                        this.saveOlympiadStatus();
                        this.init();
                     }
                  },
                  this.getMillisToCompEnd()
               );
            }
         },
         this.getMillisToCompBegin()
      );
   }

   public long getMillisToOlympiadEnd() {
      return this._olympiadEnd - System.currentTimeMillis();
   }

   public void manualSelectHeroes() {
      if (this._scheduledOlympiadEnd != null) {
         this._scheduledOlympiadEnd.cancel(true);
      }

      this._scheduledOlympiadEnd = ThreadPool.schedule(new Olympiad.OlympiadEndTask(), 0L);
   }

   protected long getMillisToValidationEnd() {
      long currentTime = System.currentTimeMillis();
      return this._validationEnd > currentTime ? this._validationEnd - currentTime : 10L;
   }

   public boolean isOlympiadEnd() {
      return this._period != 0;
   }

   protected void setNewOlympiadEnd() {
      SystemMessage sm = new SystemMessage(SystemMessageId.PERIOD_S1_OF_THE_GRAND_OLYMPIAD_GAMES_HAS_STARTED);
      sm.addInt(this._currentCycle);
      World.broadcastToAllOnlinePlayers(sm);
      if (!OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS) {
         Calendar currentTime = Calendar.getInstance();
         currentTime.add(2, 1);
         currentTime.set(5, 1);
         currentTime.set(9, 0);
         currentTime.set(11, 12);
         currentTime.set(12, 0);
         currentTime.set(13, 0);
         this._olympiadEnd = currentTime.getTimeInMillis();
         Calendar nextChange = Calendar.getInstance();
         this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
      } else {
         Calendar currentTime = Calendar.getInstance();
         currentTime.set(9, 0);
         currentTime.set(11, 12);
         currentTime.set(12, 0);
         currentTime.set(13, 0);
         Calendar nextChange = Calendar.getInstance();
         String var4 = OlympiadConfig.OLYMPIAD_PERIOD;
         switch (var4) {
            case "DAY":
               currentTime.add(5, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               currentTime.add(5, -1);
               if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 14) {
                  this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
               } else if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 7) {
                  this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD / 2L;
               } else {
                  LOGGER.warning("Invalid config value for Config.OLYMPIAD_PERIOD_MULTIPLIER, must be >= 7");
               }
               break;
            case "WEEK":
               currentTime.add(4, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               currentTime.add(5, -1);
               if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER > 1) {
                  this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
               } else {
                  this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD / 2L;
               }
               break;
            case "MONTH":
               currentTime.add(2, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               currentTime.add(5, -1);
               this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
         }

         this._olympiadEnd = currentTime.getTimeInMillis();
      }

      this.scheduleWeeklyChange();
   }

   public boolean inCompPeriod() {
      return _inCompPeriod;
   }

   private long getMillisToCompBegin() {
      long currentTime = System.currentTimeMillis();
      if (this._compStart.getTimeInMillis() < currentTime && this._compEnd > currentTime) {
         return 10L;
      } else {
         return this._compStart.getTimeInMillis() > currentTime ? this._compStart.getTimeInMillis() - currentTime : this.setNewCompBegin();
      }
   }

   private long setNewCompBegin() {
      this._compStart = Calendar.getInstance();
      int currentDay = this._compStart.get(7);
      this._compStart.set(11, COMP_START);
      this._compStart.set(12, COMP_MIN);
      if (currentDay == this._compStart.get(7)) {
         if (currentDay == 7) {
            currentDay = 1;
         } else {
            currentDay++;
         }
      }

      if (OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS) {
         boolean dayFound = false;
         int dayCounter = 0;

         for (int i = currentDay; i < 8; i++) {
            if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i)) {
               dayFound = true;
               break;
            }

            dayCounter++;
         }

         if (!dayFound) {
            for (int i = 1; i < 8 && !OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i); i++) {
               dayCounter++;
            }
         }

         if (dayCounter > 0) {
            this._compStart.add(5, dayCounter);
         }
      }

      this._compStart.add(11, 24);
      this._compEnd = this._compStart.getTimeInMillis() + COMP_PERIOD;
      LOGGER.info("Olympiad System: New Schedule @ " + this._compStart.getTime());
      return this._compStart.getTimeInMillis() - System.currentTimeMillis();
   }

   protected long getMillisToCompEnd() {
      return this._compEnd - System.currentTimeMillis();
   }

   public long getMillisToWeekChange() {
      long currentTime = System.currentTimeMillis();
      return this._nextWeeklyChange > currentTime ? this._nextWeeklyChange - currentTime : 10L;
   }

   private void scheduleWeeklyChange() {
      this._scheduledWeeklyTask = ThreadPool.scheduleAtFixedRate(() -> {
         this.addWeeklyPoints();
         LOGGER.info("Olympiad System: Added weekly points to nobles");
         Calendar nextChange = Calendar.getInstance();
         this._nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
      }, this.getMillisToWeekChange(), WEEKLY_PERIOD);
   }

   protected void addWeeklyPoints() {
      if (this._period != 1) {
         for (StatSet nobleInfo : NOBLES.values()) {
            nobleInfo.set(
               "olympiad_points",
               Math.clamp(
                  (long)(nobleInfo.getInt("olympiad_points", 0) + WEEKLY_POINTS),
                  0,
                  nobleInfo.getInt("competitions_done", 0) * OlympiadConfig.OLYMPIAD_MAX_POINTS + OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4
               )
            );
         }
      }
   }

   public Map<Integer, String> getMatchList() {
      return OlympiadManager.getInstance().getAllTitles();
   }

   public Player[] getPlayers(int id) {
      return OlympiadManager.getInstance().getOlympiadGame(id) == null ? null : OlympiadManager.getInstance().getOlympiadGame(id).getPlayers();
   }

   public int getCurrentCycle() {
      return this._currentCycle;
   }

   public static void addSpectator(int id, Player spectator, boolean storeCoords) {
      if (!getInstance().inCompPeriod()) {
         spectator.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
      } else if (getInstance().isRegisteredInComp(spectator)) {
         spectator.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_GRAND_OLYMPIAD_GAMES_MATCH_WHILE_YOU_ARE_ON_THE_WAITING_LIST);
      } else if (spectator.isRegisteredOnEvent()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_MATCH_WAITING_LIST);
         sm.addString(spectator.getName());
         spectator.sendPacket(sm);
      } else {
         OlympiadManager.STADIUMS[id].addSpectator(id, spectator, storeCoords);
         if (storeCoords) {
            handleSpectatorEnter(id, spectator);
         }
      }
   }

   public static int getSpectatorArena(Player player) {
      for (int i = 0; i < OlympiadManager.STADIUMS.length; i++) {
         if (OlympiadManager.STADIUMS[i].getSpectators().contains(player)) {
            return i;
         }
      }

      return -1;
   }

   public static void removeSpectator(int id, Player spectator) {
      try {
         OlympiadManager.STADIUMS[id].removeSpectator(spectator);
      } catch (ArrayIndexOutOfBoundsException var3) {
         LOGGER.log(Level.SEVERE, "removeSpectator()", (Throwable)var3);
      }
   }

   public List<Player> getSpectators(int id) {
      try {
         return OlympiadManager.getInstance().getOlympiadGame(id) == null ? null : OlympiadManager.STADIUMS[id].getSpectators();
      } catch (ArrayIndexOutOfBoundsException var3) {
         return null;
      }
   }

   public Map<Integer, OlympiadGame> getOlympiadGames() {
      return OlympiadManager.getInstance().getOlympiadGames();
   }

   public boolean playerInStadia(Player player) {
      return ZoneManager.getInstance().getOlympiadStadium(player) != null;
   }

   public int[] getWaitingList() {
      int[] array = new int[2];
      if (!this.inCompPeriod()) {
         return null;
      } else {
         int classCount = 0;
         if (!_classBasedRegisters.isEmpty()) {
            for (List<Player> classed : _classBasedRegisters.values()) {
               classCount += classed.size();
            }
         }

         array[0] = classCount;
         array[1] = _nonClassBasedRegisters.size();
         return array;
      }
   }

   protected synchronized void saveNobleData() {
      if (!NOBLES.isEmpty()) {
         try (Connection con = DatabaseFactory.getConnection()) {
            for (Entry<Integer, StatSet> entry : new ArrayList<>(NOBLES.entrySet())) {
               StatSet nobleInfo = entry.getValue();
               if (nobleInfo != null) {
                  int charId = entry.getKey();
                  int classId = nobleInfo.getInt("class_id");
                  int points = nobleInfo.getInt("olympiad_points");
                  int compDone = nobleInfo.getInt("competitions_done");
                  int compWon = nobleInfo.getInt("competitions_won");
                  int compLost = nobleInfo.getInt("competitions_lost");
                  int compDrawn = nobleInfo.getInt("competitions_drawn");
                  boolean toSave = nobleInfo.getBoolean("to_save");

                  try (PreparedStatement statement = con.prepareStatement(
                        toSave
                           ? "INSERT INTO olympiad_nobles (`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,`competitions_drawn`) VALUES (?,?,?,?,?,?,?)"
                           : "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?"
                     )) {
                     if (toSave) {
                        statement.setInt(1, charId);
                        statement.setInt(2, classId);
                        statement.setInt(3, points);
                        statement.setInt(4, compDone);
                        statement.setInt(5, compWon);
                        statement.setInt(6, compLost);
                        statement.setInt(7, compDrawn);
                        nobleInfo.set("to_save", false);
                     } else {
                        statement.setInt(1, points);
                        statement.setInt(2, compDone);
                        statement.setInt(3, compWon);
                        statement.setInt(4, compLost);
                        statement.setInt(5, compDrawn);
                        statement.setInt(6, charId);
                     }

                     statement.execute();
                     statement.close();
                  } catch (SQLException var20) {
                     LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noble data for charId " + charId, (Throwable)var20);
                  }
               }
            }
         } catch (SQLException var22) {
            LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noblesse data to database: ", (Throwable)var22);
         }
      }
   }

   public void saveOlympiadStatus() {
      this.saveNobleData();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?"
         );
      ) {
         statement.setInt(1, this._currentCycle);
         statement.setInt(2, this._period);
         statement.setLong(3, this._olympiadEnd);
         statement.setLong(4, this._validationEnd);
         statement.setLong(5, this._nextWeeklyChange);
         statement.setInt(6, this._currentCycle);
         statement.setInt(7, this._period);
         statement.setLong(8, this._olympiadEnd);
         statement.setLong(9, this._validationEnd);
         statement.setLong(10, this._nextWeeklyChange);
         statement.execute();
      } catch (SQLException var9) {
         LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save olympiad data to database: ", (Throwable)var9);
      }
   }

   protected void updateMonthlyData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement("TRUNCATE olympiad_nobles_eom");
         PreparedStatement ps2 = con.prepareStatement("INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles");
      ) {
         ps1.execute();
         ps2.execute();
      } catch (SQLException var12) {
         LOGGER.log(Level.SEVERE, "Olympiad System: Failed to update monthly noblese data: ", (Throwable)var12);
      }
   }

   protected void sortHerosToBe() {
      if (this._period == 1) {
         if (NOBLES != null) {
            LOGGER_OLYMPIAD.info("Noble,charid,classid,compDone,points");

            for (Entry<Integer, StatSet> entry : NOBLES.entrySet()) {
               StatSet nobleInfo = entry.getValue();
               if (nobleInfo != null) {
                  int charId = entry.getKey();
                  int classId = nobleInfo.getInt("class_id");
                  String charName = nobleInfo.getString("char_name");
                  int points = nobleInfo.getInt("olympiad_points");
                  int compDone = nobleInfo.getInt("competitions_done");
                  LogRecord record = new LogRecord(Level.INFO, charName);
                  record.setParameters(new Object[]{charId, classId, compDone, points});
                  LOGGER_OLYMPIAD.log(record);
               }
            }
         }

         HEROS_TO_BE = new ArrayList<>();

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement statement = con.prepareStatement(
               "SELECT olympiad_nobles.charId, characters.char_name FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC"
            );
         ) {
            List<StatSet> soulHounds = new ArrayList<>();

            for (int element : HERO_IDS) {
               statement.setInt(1, element);

               try (ResultSet rset = statement.executeQuery()) {
                  if (rset.next()) {
                     StatSet hero = new StatSet();
                     hero.set("class_id", element);
                     hero.set("charId", rset.getInt("charId"));
                     hero.set("char_name", rset.getString("char_name"));
                     if (element != 132 && element != 133) {
                        LogRecord record = new LogRecord(Level.INFO, "Hero " + hero.getString("char_name"));
                        record.setParameters(new Object[]{hero.getInt("charId"), hero.getInt("class_id")});
                        LOGGER_OLYMPIAD.log(record);
                        HEROS_TO_BE.add(hero);
                     } else {
                        hero = NOBLES.get(hero.getInt("charId"));
                        hero.set("charId", rset.getInt("charId"));
                        soulHounds.add(hero);
                     }
                  }
               }
            }

            switch (soulHounds.size()) {
               case 0:
               default:
                  break;
               case 1: {
                  StatSet hero = new StatSet();
                  StatSet winner = soulHounds.get(0);
                  hero.set("class_id", winner.getInt("class_id"));
                  hero.set("charId", winner.getInt("charId"));
                  hero.set("char_name", winner.getString("char_name"));
                  LogRecord record = new LogRecord(Level.INFO, "Hero " + hero.getString("char_name"));
                  record.setParameters(new Object[]{hero.getInt("charId"), hero.getInt("class_id")});
                  LOGGER_OLYMPIAD.log(record);
                  HEROS_TO_BE.add(hero);
                  break;
               }
               case 2: {
                  StatSet hero = new StatSet();
                  StatSet hero1 = soulHounds.get(0);
                  StatSet hero2 = soulHounds.get(1);
                  int hero1Points = hero1.getInt("olympiad_points");
                  int hero2Points = hero2.getInt("olympiad_points");
                  int hero1Comps = hero1.getInt("competitions_done");
                  int hero2Comps = hero2.getInt("competitions_done");
                  int hero1Wins = hero1.getInt("competitions_won");
                  int hero2Wins = hero2.getInt("competitions_won");
                  StatSet winner;
                  if (hero1Points > hero2Points) {
                     winner = hero1;
                  } else if (hero2Points > hero1Points) {
                     winner = hero2;
                  } else if (hero1Comps > hero2Comps) {
                     winner = hero1;
                  } else if (hero2Comps > hero1Comps) {
                     winner = hero2;
                  } else if (hero1Wins > hero2Wins) {
                     winner = hero1;
                  } else {
                     winner = hero2;
                  }

                  hero.set("class_id", winner.getInt("class_id"));
                  hero.set("charId", winner.getInt("charId"));
                  hero.set("char_name", winner.getString("char_name"));
                  LogRecord record = new LogRecord(Level.INFO, "Hero " + hero.getString("char_name"));
                  record.setParameters(new Object[]{hero.getInt("charId"), hero.getInt("class_id")});
                  LOGGER_OLYMPIAD.log(record);
                  HEROS_TO_BE.add(hero);
               }
            }
         } catch (SQLException var21) {
            LOGGER.warning("Olympiad System: Could not load heros from DB");
         }
      }
   }

   public List<String> getClassLeaderBoard(int classId) {
      List<String> names = new ArrayList<>();
      String query = OlympiadConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS
         ? (
            classId == 132
               ? "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = 133) AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10"
               : "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10"
         )
         : (
            classId == 132
               ? "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = 133) AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10"
               : "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10"
         );

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(query);
      ) {
         ps.setInt(1, classId);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               names.add(rset.getString("char_name"));
            }
         }
      } catch (SQLException var15) {
         LOGGER.warning("Olympiad System: Couldn't load olympiad leaders from DB!");
      }

      return names;
   }

   private int getNoblessePasses(int objectId) {
      if (this._period == 1 && !NOBLES_RANK.isEmpty()) {
         if (!NOBLES_RANK.containsKey(objectId)) {
            return 0;
         } else {
            StatSet noble = NOBLES.get(objectId);
            if (noble != null && noble.getInt("olympiad_points") != 0) {
               int points = Hero.getInstance().isHero(objectId) ? OlympiadConfig.OLYMPIAD_HERO_POINTS : 0;

               points = switch (NOBLES_RANK.get(objectId)) {
                  case 1 -> points + OlympiadConfig.OLYMPIAD_RANK1_POINTS;
                  case 2 -> points + OlympiadConfig.OLYMPIAD_RANK2_POINTS;
                  case 3 -> points + OlympiadConfig.OLYMPIAD_RANK3_POINTS;
                  case 4 -> points + OlympiadConfig.OLYMPIAD_RANK4_POINTS;
                  default -> points + OlympiadConfig.OLYMPIAD_RANK5_POINTS;
               } * OlympiadConfig.OLYMPIAD_GP_PER_POINT;
               noble.set("olympiad_points", 0);
               return points;
            } else {
               return 0;
            }
         }
      } else {
         return 0;
      }
   }

   public boolean isRegisteredInComp(Player player) {
      boolean result = this.isRegistered(player);
      if (_inCompPeriod) {
         for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values()) {
            if (game._playerOneID == player.getObjectId() || game._playerTwoID == player.getObjectId()) {
               result = true;
               break;
            }
         }
      }

      return result;
   }

   public int getNoblePoints(int objId) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet noble = NOBLES.get(objId);
         return noble == null ? 0 : noble.getInt("olympiad_points");
      }
   }

   public void setNoblePoints(int objId, int points) {
      if (!NOBLES.isEmpty()) {
         StatSet noble = NOBLES.get(objId);
         if (noble != null) {
            noble.set("olympiad_points", Math.max(0, points));
         }
      }
   }

   public int getLastNobleOlympiadPoints(int objId) {
      int result = 0;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?");
      ) {
         ps.setInt(1, objId);

         try (ResultSet rs = ps.executeQuery()) {
            if (rs.first()) {
               result = rs.getInt(1);
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Could not load last olympiad points:", (Throwable)var14);
      }

      return result;
   }

   public int getCompetitionDone(int objId) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet noble = NOBLES.get(objId);
         return noble == null ? 0 : noble.getInt("competitions_done");
      }
   }

   public int getCompetitionWon(int objId) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet noble = NOBLES.get(objId);
         return noble == null ? 0 : noble.getInt("competitions_won");
      }
   }

   public int getCompetitionLost(int objId) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet noble = NOBLES.get(objId);
         return noble == null ? 0 : noble.getInt("competitions_lost");
      }
   }

   protected void deleteNobles() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("TRUNCATE olympiad_nobles");
      ) {
         statement.execute();
      } catch (SQLException var9) {
         LOGGER.warning("Olympiad System: Couldn't delete nobles from DB!");
      }

      NOBLES.clear();
   }

   public static void sendMatchList(Player player) {
      NpcHtmlMessage message = new NpcHtmlMessage(0);
      message.setFile(player, "data/html/olympiad/olympiad_observe2.htm");
      Map<Integer, String> matches = getInstance().getMatchList();

      for (int i = 0; i < getStadiumCount(); i++) {
         int arenaId = i + 1;
         String state = "Initial State";
         String players = "&nbsp;";
         String matchPlayers = matches.get(i);
         if (matchPlayers != null) {
            if (OlympiadGame._gameIsStarted) {
               state = "Playing";
            } else {
               state = "Standby";
            }

            players = matchPlayers;
         }

         message.replace("%state" + arenaId + "%", state);
         message.replace("%players" + arenaId + "%", players);
      }

      player.sendPacket(message);
   }

   private static void handleSpectatorEnter(int gameId, Player player) {
      OlympiadGame game = OlympiadManager.getInstance().getOlympiadGame(gameId);
      if (game != null) {
         if (game._playerOne == null || !game._playerOne.isInOlympiadMode() || !game._playerOne.isOlympiadStart()) {
            return;
         }

         if (game._playerTwo == null || !game._playerTwo.isInOlympiadMode() || !game._playerTwo.isOlympiadStart()) {
            return;
         }

         player.sendPacket(new ExOlympiadUserInfo(game._playerOne, 1));
         player.sendPacket(new ExOlympiadUserInfo(game._playerTwo, 2));
      }
   }

   public static void bypassChangeArena(String command, Player player) {
      if (player.inObserverMode()) {
         String[] commands = command.split(" ");
         int id = Integer.parseInt(commands[1]);
         int arena = getSpectatorArena(player);
         if (arena >= 0) {
            removeSpectator(arena, player);
            player.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
            addSpectator(id, player, false);
            handleSpectatorEnter(id, player);
         }
      }
   }

   public static Olympiad getInstance() {
      return Olympiad.SingletonHolder._instance;
   }

   protected class OlympiadEndTask implements Runnable {
      protected OlympiadEndTask() {
         Objects.requireNonNull(Olympiad.this);
         super();
      }

      @Override
      public void run() {
         SystemMessage sm = new SystemMessage(SystemMessageId.PERIOD_S1_OF_THE_GRAND_OLYMPIAD_GAMES_HAS_NOW_ENDED);
         sm.addInt(Olympiad.this._currentCycle);
         World.broadcastToAllOnlinePlayers(sm);
         World.broadcastToAllOnlinePlayers("Olympiad Validation Period has began");
         if (Olympiad.this._scheduledWeeklyTask != null) {
            Olympiad.this._scheduledWeeklyTask.cancel(true);
         }

         Olympiad.this.saveNobleData();
         Olympiad.this._period = 1;
         Olympiad.this.sortHerosToBe();
         Hero.getInstance().resetData();
         Hero.getInstance().computeNewHeroes(Olympiad.HEROS_TO_BE);
         Olympiad.this.saveOlympiadStatus();
         Olympiad.this.updateMonthlyData();
         Calendar validationEnd = Calendar.getInstance();
         Olympiad.this._validationEnd = validationEnd.getTimeInMillis() + Olympiad.VALIDATION_PERIOD;
         Olympiad.this.loadNoblesRank();
         Olympiad.this._scheduledValdationTask = ThreadPool.schedule(Olympiad.this.new ValidationEndTask(), Olympiad.this.getMillisToValidationEnd());
      }
   }

   private static class SingletonHolder {
      protected static final Olympiad _instance = new Olympiad();
   }

   protected class ValidationEndTask implements Runnable {
      protected ValidationEndTask() {
         Objects.requireNonNull(Olympiad.this);
         super();
      }

      @Override
      public void run() {
         World.broadcastToAllOnlinePlayers("Olympiad Validation Period has ended");
         Olympiad.this._period = 0;
         Olympiad.this._currentCycle++;
         Olympiad.this.deleteNobles();
         Olympiad.this.setNewOlympiadEnd();
         Olympiad.this.init();
      }
   }
}
