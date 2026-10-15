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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.OlympiadEndTask;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.SingletonHolder;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.ValidationEndTask;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class Olympiad extends ListenersContainer {
   protected static final Logger LOGGER = Logger.getLogger(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.class.getName());
   protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
   private static final Map<Integer, StatSet> NOBLES = new ConcurrentHashMap<>();
   protected static List<StatSet> HEROS_TO_BE;
   private static Map<Integer, Integer> NOBLES_RANK = new HashMap<>();
   private static List<Player> _nonClassBasedRegisters;
   private static Map<Integer, List<Player>> _classBasedRegisters;
   public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
   public static final List<Consumer<Player>> PRE_START_HOOKS = new CopyOnWriteArrayList<>();
   public static final List<Predicate<Player>> DEAD_PARTICIPANT_REVIVE_HOOKS = new CopyOnWriteArrayList<>();
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
      boolean var1 = false;

      try (
         Connection var2 = DatabaseFactory.getConnection();
         PreparedStatement var3 = var2.prepareStatement(
            "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0"
         );
         ResultSet var4 = var3.executeQuery();
      ) {
         while (var4.next()) {
            this._currentCycle = var4.getInt("current_cycle");
            this._period = var4.getInt("period");
            this._olympiadEnd = var4.getLong("olympiad_end");
            this._validationEnd = var4.getLong("validation_end");
            this._nextWeeklyChange = var4.getLong("next_weekly_change");
            var1 = true;
         }
      } catch (Exception var33) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading olympiad data from database: ", (Throwable)var33);
      }

      if (!var1) {
         this._currentCycle = 1;
         this._period = 0;
         this._olympiadEnd = 0L;
         this._validationEnd = 0L;
         this._nextWeeklyChange = 0L;
      }

      long var34 = System.currentTimeMillis();
      switch (this._period) {
         case 0:
            if (this._olympiadEnd != 0L && this._olympiadEnd >= var34) {
               this.scheduleWeeklyChange();
            } else {
               this.setNewOlympiadEnd();
            }
            break;
         case 1:
            if (this._validationEnd > var34) {
               this.loadNoblesRank();
               this._scheduledValdationTask = ThreadPool.schedule(new ValidationEndTask(this), this.getMillisToValidationEnd());
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
         Connection var35 = DatabaseFactory.getConnection();
         PreparedStatement var5 = var35.prepareStatement(
            "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId"
         );
         ResultSet var6 = var5.executeQuery();
      ) {
         while (var6.next()) {
            StatSet var7 = new StatSet();
            int var8 = var6.getInt("charId");
            var7.set("class_id", var6.getInt("class_id"));
            var7.set("char_name", var6.getString("char_name"));
            int var9 = var6.getInt("competitions_done");
            var7.set(
               "olympiad_points",
               Math.clamp((long)var6.getInt("olympiad_points"), 0, OlympiadConfig.OLYMPIAD_MAX_POINTS * var9 + OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4)
            );
            var7.set("competitions_done", var9);
            var7.set("competitions_won", var6.getInt("competitions_won"));
            var7.set("competitions_lost", var6.getInt("competitions_lost"));
            var7.set("competitions_drawn", var6.getInt("competitions_drawn"));
            var7.set("to_save", false);
            NOBLES.put(var8, var7);
         }

         var6.close();
         var5.close();
      } catch (Exception var29) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database: ", (Throwable)var29);
      }

      LOGGER.info("Olympiad System: Loading Olympiad System....");
      if (this._period == 0) {
         LOGGER.info("Olympiad System: Currently in Olympiad Period");
      } else {
         LOGGER.info("Olympiad System: Currently in Validation Period");
      }

      long var36;
      if (this._period == 0) {
         var36 = this.getMillisToOlympiadEnd();
      } else {
         var36 = this.getMillisToValidationEnd();
      }

      double var38 = var36 / 1000L % 60L;
      double var39 = (var36 / 1000.0 - var38) / 60.0;
      int var10 = (int)(var39 % 60.0);
      var39 = (var39 - var10) / 60.0;
      int var11 = (int)(var39 % 24.0);
      int var12 = (int)((var39 - var11) / 24.0);
      LOGGER.info("Olympiad System: " + var12 + " days, " + var11 + " hours and " + var10 + " mins until period ends.");
      if (this._period == 0) {
         var36 = this.getMillisToWeekChange();
         double var13 = var36 / 1000L % 60L;
         double var15 = (var36 / 1000.0 - var13) / 60.0;
         int var17 = (int)(var39 % 60.0);
         var15 = (var15 - var10) / 60.0;
         int var18 = (int)(var15 % 24.0);
         int var19 = (int)((var15 - var11) / 24.0);
         LOGGER.info("Olympiad System: Next weekly change is in " + var19 + " days, " + var18 + " hours and " + var17 + " mins.");
      }

      LOGGER.info("Olympiad System: Loaded " + NOBLES.size() + " Nobles");
   }

   public void loadNoblesRank() {
      NOBLES_RANK.clear();
      HashMap var1 = new HashMap();

      try (
         Connection var2 = DatabaseFactory.getConnection();
         PreparedStatement var3 = var2.prepareStatement(
            "SELECT charId from olympiad_nobles_eom WHERE competitions_done >= 9 ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC"
         );
         ResultSet var4 = var3.executeQuery();
      ) {
         int var5 = 1;

         while (var4.next()) {
            var1.put(var4.getInt("charId"), var5++);
         }
      } catch (Exception var30) {
         LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database for Ranking: ", (Throwable)var30);
      }

      int var31 = (int)Math.round(var1.size() * 0.01);
      int var32 = (int)Math.round(var1.size() * 0.1);
      int var33 = (int)Math.round(var1.size() * 0.25);
      int var34 = (int)Math.round(var1.size() * 0.5);
      if (var31 == 0) {
         var31 = 1;
         var32++;
         var33++;
         var34++;
      }

      for (Entry var7 : var1.entrySet()) {
         if ((Integer)var7.getValue() <= var31) {
            NOBLES_RANK.put((Integer)var7.getKey(), 1);
         } else if ((Integer)var1.get(var7.getKey()) <= var32) {
            NOBLES_RANK.put((Integer)var7.getKey(), 2);
         } else if ((Integer)var1.get(var7.getKey()) <= var33) {
            NOBLES_RANK.put((Integer)var7.getKey(), 3);
         } else if ((Integer)var1.get(var7.getKey()) <= var34) {
            NOBLES_RANK.put((Integer)var7.getKey(), 4);
         } else {
            NOBLES_RANK.put((Integer)var7.getKey(), 5);
         }
      }

      for (int var36 : NOBLES.keySet()) {
         int var8 = this.getNoblessePasses(var36);
         if (var8 > 0) {
            Player var9 = World.getPlayer(var36);
            if (var9 != null) {
               var9.getVariables().set("UNCLAIMED_OLYMPIAD_POINTS", var8);
            } else {
               try (
                  Connection var10 = DatabaseFactory.getConnection();
                  PreparedStatement var11 = var10.prepareStatement("DELETE FROM character_variables WHERE charId=? AND var=?");
               ) {
                  var11.setInt(1, var36);
                  var11.setString(2, "UNCLAIMED_OLYMPIAD_POINTS");
                  var11.execute();
               } catch (SQLException var26) {
                  LOGGER.warning("Olympiad System: Couldn't remove unclaimed olympiad points from DB!");
               }

               try (
                  Connection var37 = DatabaseFactory.getConnection();
                  PreparedStatement var38 = var37.prepareStatement("INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)");
               ) {
                  var38.setInt(1, var36);
                  var38.setString(2, "UNCLAIMED_OLYMPIAD_POINTS");
                  var38.setString(3, String.valueOf(var8));
                  var38.execute();
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
            int var1 = this._compStart.get(7);
            boolean var2 = false;
            int var3 = 0;

            for (int var4 = var1; var4 < 8; var4++) {
               if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(var4)) {
                  var2 = true;
                  break;
               }

               var3++;
            }

            if (!var2) {
               for (int var5 = 1; var5 < 8 && !OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(var5); var5++) {
                  var3++;
               }
            }

            if (var3 > 0) {
               this._compStart.add(5, var3);
            }
         }

         this._compStart.set(11, COMP_START);
         this._compStart.set(12, COMP_MIN);
         this._compEnd = this._compStart.getTimeInMillis() + COMP_PERIOD;
         if (this._scheduledOlympiadEnd != null) {
            this._scheduledOlympiadEnd.cancel(true);
         }

         this._scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(this), this.getMillisToOlympiadEnd());
         this.updateCompStatus();
      }
   }

   public boolean registerNoble(Player var1, boolean var2) {
      if (var1.getOlympiadGameId() != -1) {
         return false;
      } else if (!_inCompPeriod) {
         SystemMessage var16 = new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
         var1.sendPacket(var16);
         return false;
      } else if (!var1.isNoble()) {
         SystemMessage var15 = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
         var15.addPcName(var1);
         var1.sendPacket(var15);
         return false;
      } else if (var1.getBaseClass() != var1.getPlayerClass().getId()) {
         SystemMessage var14 = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_A_SUB_CLASS_CHARACTER_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD
         );
         var14.addPcName(var1);
         var1.sendPacket(var14);
         return false;
      } else if (var1.isCursedWeaponEquipped()) {
         SystemMessage var13 = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD
         );
         var13.addPcName(var1);
         var13.addItemName(var1.getCursedWeaponEquippedId());
         var1.sendPacket(var13);
         return false;
      } else if (var1.getInventoryLimit() * 0.8 <= var1.getInventory().getSize()) {
         SystemMessage var12 = new SystemMessage(
            SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80
         );
         var12.addPcName(var1);
         var1.sendPacket(var12);
         return false;
      } else if (var1.isRegisteredOnEvent()) {
         var1.sendMessage("You can't join olympiad while participating on events.");
         return false;
      } else {
         if (_classBasedRegisters.containsKey(var1.getPlayerClass().getId())) {
            for (Player var4 : _classBasedRegisters.get(var1.getPlayerClass().getId())) {
               if (var4.getObjectId() == var1.getObjectId()) {
                  SystemMessage var5 = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
                  var5.addPcName(var1);
                  var1.sendPacket(var5);
                  return false;
               }
            }
         }

         if (this.isRegisteredInComp(var1)) {
            SystemMessage var11 = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_CLASS_IRRELEVANT_INDIVIDUAL_MATCH);
            var11.addPcName(var1);
            var1.sendPacket(var11);
            return false;
         } else {
            if (!NOBLES.containsKey(var1.getObjectId())) {
               StatSet var6 = new StatSet();
               var6.set("class_id", var1.getPlayerClass().getId());
               var6.set("char_name", var1.getName());
               var6.set("olympiad_points", DEFAULT_POINTS);
               var6.set("competitions_done", 0);
               var6.set("competitions_won", 0);
               var6.set("competitions_lost", 0);
               var6.set("competitions_drawn", 0);
               var6.set("to_save", true);
               NOBLES.put(var1.getObjectId(), var6);
            }

            if (var2 && this.getNoblePoints(var1.getObjectId()) < 3) {
               var1.sendMessage("Cannot register when you have less than 3 points");
               return false;
            } else if (!var2 && this.getNoblePoints(var1.getObjectId()) < 5) {
               var1.sendMessage("Cannot register when you have less than 5 points");
               return false;
            } else {
               if (var2) {
                  if (_classBasedRegisters.containsKey(var1.getPlayerClass().getId())) {
                     List var7 = _classBasedRegisters.get(var1.getPlayerClass().getId());
                     var7.add(var1);
                     _classBasedRegisters.remove(var1.getPlayerClass().getId());
                     _classBasedRegisters.put(var1.getPlayerClass().getId(), var7);
                  } else {
                     ArrayList var8 = new ArrayList();
                     var8.add(var1);
                     _classBasedRegisters.put(var1.getPlayerClass().getId(), var8);
                  }

                  SystemMessage var9 = new SystemMessage(
                     SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST_FOR_A_CLASS_SPECIFIC_MATCH
                  );
                  var1.sendPacket(var9);
               } else {
                  _nonClassBasedRegisters.add(var1);
                  SystemMessage var10 = new SystemMessage(SystemMessageId.YOU_ARE_CURRENTLY_REGISTERED_FOR_A_1V1_CLASS_IRRELEVANT_MATCH);
                  var1.sendPacket(var10);
               }

               return true;
            }
         }
      }
   }

   protected static int getNobleCount() {
      return NOBLES.size();
   }

   protected static StatSet getNobleStats(int var0) {
      return NOBLES.get(var0);
   }

   protected static List<Player> getRegisteredNonClassBased() {
      return _nonClassBasedRegisters;
   }

   protected static Map<Integer, List<Player>> getRegisteredClassBased() {
      return _classBasedRegisters;
   }

   protected static List<Integer> hasEnoughRegisteredClassed() {
      ArrayList var0 = new ArrayList();

      for (Integer var2 : getRegisteredClassBased().keySet()) {
         if (getRegisteredClassBased().get(var2).size() >= OlympiadConfig.OLYMPIAD_CLASSED) {
            var0.add(var2);
         }
      }

      return !var0.isEmpty() ? var0 : null;
   }

   protected static boolean hasEnoughRegisteredNonClassed() {
      return getRegisteredNonClassBased().size() >= OlympiadConfig.OLYMPIAD_NONCLASSED;
   }

   protected static void clearRegistered() {
      _nonClassBasedRegisters.clear();
      _classBasedRegisters.clear();
   }

   public boolean isRegistered(Player var1) {
      boolean var2 = false;
      if (_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(var1)) {
         var2 = true;
      } else if (_classBasedRegisters != null && _classBasedRegisters.containsKey(var1.getPlayerClass().getId())) {
         List var3 = _classBasedRegisters.get(var1.getPlayerClass().getId());
         if (var3 != null && var3.contains(var1)) {
            var2 = true;
         }
      }

      return var2;
   }

   public boolean unRegisterNoble(Player var1) {
      if (!_inCompPeriod) {
         SystemMessage var8 = new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
         var1.sendPacket(var8);
         return false;
      } else if (!var1.isNoble()) {
         SystemMessage var7 = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
         var7.addString(var1.getName());
         var1.sendPacket(var7);
         return false;
      } else if (!this.isRegistered(var1)) {
         SystemMessage var6 = new SystemMessage(SystemMessageId.YOU_ARE_NOT_CURRENTLY_REGISTERED_ON_ANY_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
         var1.sendPacket(var6);
         return false;
      } else {
         for (OlympiadGame var3 : OlympiadManager.getInstance().getOlympiadGames().values()) {
            if (var3 != null && (var3._playerOneID == var1.getObjectId() || var3._playerTwoID == var1.getObjectId())) {
               var1.sendMessage("Can't deregister whilst you are already selected for a game");
               return false;
            }
         }

         if (_nonClassBasedRegisters.contains(var1)) {
            _nonClassBasedRegisters.remove(var1);
         } else {
            List var4 = _classBasedRegisters.get(var1.getPlayerClass().getId());
            var4.remove(var1);
            _classBasedRegisters.remove(var1.getPlayerClass().getId());
            _classBasedRegisters.put(var1.getPlayerClass().getId(), var4);
         }

         SystemMessage var5 = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REMOVED_FROM_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
         var1.sendPacket(var5);
         if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) {
            AntiFeedManager.getInstance().removePlayer(1, var1);
         }

         return true;
      }
   }

   public void removeDisconnectedCompetitor(Player var1) {
      if (OlympiadManager.getInstance().getOlympiadGame(var1.getOlympiadGameId()) != null) {
         OlympiadManager.getInstance().getOlympiadGame(var1.getOlympiadGameId()).handleDisconnect(var1);
      }

      List var2 = _classBasedRegisters.get(var1.getPlayerClass().getId());
      if (_nonClassBasedRegisters.contains(var1)) {
         _nonClassBasedRegisters.remove(var1);
      } else if (var2 != null && var2.contains(var1)) {
         var2.remove(var1);
         _classBasedRegisters.remove(var1.getPlayerClass().getId());
         _classBasedRegisters.put(var1.getPlayerClass().getId(), var2);
      }
   }

   public void notifyCompetitorDamage(Player var1, int var2, int var3) {
      if (OlympiadManager.getInstance().getOlympiadGames().get(var3) != null) {
         ((OlympiadGame)OlympiadManager.getInstance().getOlympiadGames().get(var3)).addDamage(var1, var2);
      }
   }

   private void updateCompStatus() {
      long var1 = this.getMillisToCompBegin();
      double var3 = var1 / 1000L % 60L;
      double var5 = (var1 / 1000L - var3) / 60.0;
      int var7 = (int)(var5 % 60.0);
      var5 = (var5 - var7) / 60.0;
      int var8 = (int)(var5 % 24.0);
      int var9 = (int)((var5 - var8) / 24.0);
      LOGGER.info("Olympiad System: Competition Period Starts in " + var9 + " days, " + var8 + " hours and " + var7 + " mins.");
      LOGGER.info("Olympiad System: Event starts/started : " + this._compStart.getTime());
      this._scheduledCompStart = ThreadPool.schedule(
         () -> {
            if (!this.isOlympiadEnd()) {
               _inCompPeriod = true;
               OlympiadManager var1x = OlympiadManager.getInstance();
               World.broadcastToAllOnlinePlayers(
                  new SystemMessage(
                     SystemMessageId.SHARPEN_YOUR_SWORDS_TIGHTEN_THE_STITCHINGS_IN_YOUR_ARMOR_AND_MAKE_HASTE_TO_A_GRAND_OLYMPIAD_MANAGER_BATTLES_IN_THE_GRAND_OLYMPIAD_GAMES_ARE_NOW_TAKING_PLACE
                  )
               );
               LOGGER.info("Olympiad System: Olympiad Game Started");
               LOGGER_OLYMPIAD.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
               Thread var2 = new Thread(var1x);
               var2.start();
               long var3x = this.getMillisToCompEnd() - 600000L;
               if (var3x > 0L) {
                  ThreadPool.schedule(
                     () -> World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_REGISTRATION_PERIOD_HAS_ENDED)), var3x
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

      this._scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(this), 0L);
   }

   protected long getMillisToValidationEnd() {
      long var1 = System.currentTimeMillis();
      return this._validationEnd > var1 ? this._validationEnd - var1 : 10L;
   }

   public boolean isOlympiadEnd() {
      return this._period != 0;
   }

   protected void setNewOlympiadEnd() {
      SystemMessage var1 = new SystemMessage(SystemMessageId.PERIOD_S1_OF_THE_GRAND_OLYMPIAD_GAMES_HAS_STARTED);
      var1.addInt(this._currentCycle);
      World.broadcastToAllOnlinePlayers(var1);
      if (!OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS) {
         Calendar var2 = Calendar.getInstance();
         var2.add(2, 1);
         var2.set(5, 1);
         var2.set(9, 0);
         var2.set(11, 12);
         var2.set(12, 0);
         var2.set(13, 0);
         this._olympiadEnd = var2.getTimeInMillis();
         Calendar var3 = Calendar.getInstance();
         this._nextWeeklyChange = var3.getTimeInMillis() + WEEKLY_PERIOD;
      } else {
         Calendar var7 = Calendar.getInstance();
         var7.set(9, 0);
         var7.set(11, 12);
         var7.set(12, 0);
         var7.set(13, 0);
         Calendar var8 = Calendar.getInstance();
         String var4 = OlympiadConfig.OLYMPIAD_PERIOD;
         switch (var4) {
            case "DAY":
               var7.add(5, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               var7.add(5, -1);
               if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 14) {
                  this._nextWeeklyChange = var8.getTimeInMillis() + WEEKLY_PERIOD;
               } else if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 7) {
                  this._nextWeeklyChange = var8.getTimeInMillis() + WEEKLY_PERIOD / 2L;
               } else {
                  LOGGER.warning("Invalid config value for Config.OLYMPIAD_PERIOD_MULTIPLIER, must be >= 7");
               }
               break;
            case "WEEK":
               var7.add(4, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               var7.add(5, -1);
               if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER > 1) {
                  this._nextWeeklyChange = var8.getTimeInMillis() + WEEKLY_PERIOD;
               } else {
                  this._nextWeeklyChange = var8.getTimeInMillis() + WEEKLY_PERIOD / 2L;
               }
               break;
            case "MONTH":
               var7.add(2, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
               var7.add(5, -1);
               this._nextWeeklyChange = var8.getTimeInMillis() + WEEKLY_PERIOD;
         }

         this._olympiadEnd = var7.getTimeInMillis();
      }

      this.scheduleWeeklyChange();
   }

   public boolean inCompPeriod() {
      return _inCompPeriod;
   }

   private long getMillisToCompBegin() {
      long var1 = System.currentTimeMillis();
      if (this._compStart.getTimeInMillis() < var1 && this._compEnd > var1) {
         return 10L;
      } else {
         return this._compStart.getTimeInMillis() > var1 ? this._compStart.getTimeInMillis() - var1 : this.setNewCompBegin();
      }
   }

   private long setNewCompBegin() {
      this._compStart = Calendar.getInstance();
      int var1 = this._compStart.get(7);
      this._compStart.set(11, COMP_START);
      this._compStart.set(12, COMP_MIN);
      if (var1 == this._compStart.get(7)) {
         if (var1 == 7) {
            var1 = 1;
         } else {
            var1++;
         }
      }

      if (OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS) {
         boolean var2 = false;
         int var3 = 0;

         for (int var4 = var1; var4 < 8; var4++) {
            if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(var4)) {
               var2 = true;
               break;
            }

            var3++;
         }

         if (!var2) {
            for (int var5 = 1; var5 < 8 && !OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(var5); var5++) {
               var3++;
            }
         }

         if (var3 > 0) {
            this._compStart.add(5, var3);
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
      long var1 = System.currentTimeMillis();
      return this._nextWeeklyChange > var1 ? this._nextWeeklyChange - var1 : 10L;
   }

   private void scheduleWeeklyChange() {
      this._scheduledWeeklyTask = ThreadPool.scheduleAtFixedRate(() -> {
         this.addWeeklyPoints();
         LOGGER.info("Olympiad System: Added weekly points to nobles");
         Calendar var1 = Calendar.getInstance();
         this._nextWeeklyChange = var1.getTimeInMillis() + WEEKLY_PERIOD;
      }, this.getMillisToWeekChange(), WEEKLY_PERIOD);
   }

   protected void addWeeklyPoints() {
      if (this._period != 1) {
         for (StatSet var2 : NOBLES.values()) {
            var2.set(
               "olympiad_points",
               Math.clamp(
                  (long)(var2.getInt("olympiad_points", 0) + WEEKLY_POINTS),
                  0,
                  var2.getInt("competitions_done", 0) * OlympiadConfig.OLYMPIAD_MAX_POINTS + OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4
               )
            );
         }
      }
   }

   public Map<Integer, String> getMatchList() {
      return OlympiadManager.getInstance().getAllTitles();
   }

   public Player[] getPlayers(int var1) {
      return OlympiadManager.getInstance().getOlympiadGame(var1) == null ? null : OlympiadManager.getInstance().getOlympiadGame(var1).getPlayers();
   }

   public int getCurrentCycle() {
      return this._currentCycle;
   }

   public static void addSpectator(int var0, Player var1, boolean var2) {
      if (!getInstance().inCompPeriod()) {
         var1.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
      } else if (getInstance().isRegisteredInComp(var1)) {
         var1.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_GRAND_OLYMPIAD_GAMES_MATCH_WHILE_YOU_ARE_ON_THE_WAITING_LIST);
      } else if (var1.isRegisteredOnEvent()) {
         SystemMessage var3 = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_MATCH_WAITING_LIST);
         var3.addString(var1.getName());
         var1.sendPacket(var3);
      } else {
         OlympiadManager.STADIUMS[var0].addSpectator(var0, var1, var2);
         if (var2) {
            handleSpectatorEnter(var0, var1);
         }
      }
   }

   public static int getSpectatorArena(Player var0) {
      for (int var1 = 0; var1 < OlympiadManager.STADIUMS.length; var1++) {
         if (OlympiadManager.STADIUMS[var1].getSpectators().contains(var0)) {
            return var1;
         }
      }

      return -1;
   }

   public static void removeSpectator(int var0, Player var1) {
      try {
         OlympiadManager.STADIUMS[var0].removeSpectator(var1);
      } catch (ArrayIndexOutOfBoundsException var3) {
         LOGGER.log(Level.SEVERE, "removeSpectator()", (Throwable)var3);
      }
   }

   public List<Player> getSpectators(int var1) {
      try {
         return OlympiadManager.getInstance().getOlympiadGame(var1) == null ? null : OlympiadManager.STADIUMS[var1].getSpectators();
      } catch (ArrayIndexOutOfBoundsException var3) {
         return null;
      }
   }

   public Map<Integer, OlympiadGame> getOlympiadGames() {
      return OlympiadManager.getInstance().getOlympiadGames();
   }

   public boolean playerInStadia(Player var1) {
      return ZoneManager.getInstance().getOlympiadStadium(var1) != null;
   }

   public int[] getWaitingList() {
      int[] var1 = new int[2];
      if (!this.inCompPeriod()) {
         return null;
      } else {
         int var2 = 0;
         if (!_classBasedRegisters.isEmpty()) {
            for (List var4 : _classBasedRegisters.values()) {
               var2 += var4.size();
            }
         }

         var1[0] = var2;
         var1[1] = _nonClassBasedRegisters.size();
         return var1;
      }
   }

   protected synchronized void saveNobleData() {
      if (!NOBLES.isEmpty()) {
         try (Connection var1 = DatabaseFactory.getConnection()) {
            for (Entry var3 : new ArrayList<>(NOBLES.entrySet())) {
               StatSet var4 = (StatSet)var3.getValue();
               if (var4 != null) {
                  int var5 = (Integer)var3.getKey();
                  int var6 = var4.getInt("class_id");
                  int var7 = var4.getInt("olympiad_points");
                  int var8 = var4.getInt("competitions_done");
                  int var9 = var4.getInt("competitions_won");
                  int var10 = var4.getInt("competitions_lost");
                  int var11 = var4.getInt("competitions_drawn");
                  boolean var12 = var4.getBoolean("to_save");

                  try (PreparedStatement var13 = var1.prepareStatement(
                        var12
                           ? "INSERT INTO olympiad_nobles (`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,`competitions_drawn`) VALUES (?,?,?,?,?,?,?)"
                           : "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?"
                     )) {
                     if (var12) {
                        var13.setInt(1, var5);
                        var13.setInt(2, var6);
                        var13.setInt(3, var7);
                        var13.setInt(4, var8);
                        var13.setInt(5, var9);
                        var13.setInt(6, var10);
                        var13.setInt(7, var11);
                        var4.set("to_save", false);
                     } else {
                        var13.setInt(1, var7);
                        var13.setInt(2, var8);
                        var13.setInt(3, var9);
                        var13.setInt(4, var10);
                        var13.setInt(5, var11);
                        var13.setInt(6, var5);
                     }

                     var13.execute();
                     var13.close();
                  } catch (SQLException var19) {
                     LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noble data for charId " + var5, (Throwable)var19);
                  }
               }
            }
         } catch (SQLException var21) {
            LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noblesse data to database: ", (Throwable)var21);
         }
      }
   }

   public void saveOlympiadStatus() {
      this.saveNobleData();

      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement(
            "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?"
         );
      ) {
         var2.setInt(1, this._currentCycle);
         var2.setInt(2, this._period);
         var2.setLong(3, this._olympiadEnd);
         var2.setLong(4, this._validationEnd);
         var2.setLong(5, this._nextWeeklyChange);
         var2.setInt(6, this._currentCycle);
         var2.setInt(7, this._period);
         var2.setLong(8, this._olympiadEnd);
         var2.setLong(9, this._validationEnd);
         var2.setLong(10, this._nextWeeklyChange);
         var2.execute();
      } catch (SQLException var9) {
         LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save olympiad data to database: ", (Throwable)var9);
      }
   }

   protected void updateMonthlyData() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("TRUNCATE olympiad_nobles_eom");
         PreparedStatement var3 = var1.prepareStatement("INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles");
      ) {
         var2.execute();
         var3.execute();
      } catch (SQLException var12) {
         LOGGER.log(Level.SEVERE, "Olympiad System: Failed to update monthly noblese data: ", (Throwable)var12);
      }
   }

   protected void sortHerosToBe() {
      if (this._period == 1) {
         if (NOBLES != null) {
            LOGGER_OLYMPIAD.info("Noble,charid,classid,compDone,points");

            for (Entry var2 : NOBLES.entrySet()) {
               StatSet var3 = (StatSet)var2.getValue();
               if (var3 != null) {
                  int var4 = (Integer)var2.getKey();
                  int var5 = var3.getInt("class_id");
                  String var6 = var3.getString("char_name");
                  int var7 = var3.getInt("olympiad_points");
                  int var8 = var3.getInt("competitions_done");
                  LogRecord var9 = new LogRecord(Level.INFO, var6);
                  var9.setParameters(new Object[]{var4, var5, var8, var7});
                  LOGGER_OLYMPIAD.log(var9);
               }
            }
         }

         HEROS_TO_BE = new ArrayList<>();

         try (
            Connection var25 = DatabaseFactory.getConnection();
            PreparedStatement var26 = var25.prepareStatement(
               "SELECT olympiad_nobles.charId, characters.char_name FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC"
            );
         ) {
            ArrayList var27 = new ArrayList();

            for (int var34 : HERO_IDS) {
               var26.setInt(1, var34);

               try (ResultSet var36 = var26.executeQuery()) {
                  if (var36.next()) {
                     StatSet var38 = new StatSet();
                     var38.set("class_id", var34);
                     var38.set("charId", var36.getInt("charId"));
                     var38.set("char_name", var36.getString("char_name"));
                     if (var34 != 132 && var34 != 133) {
                        LogRecord var10 = new LogRecord(Level.INFO, "Hero " + var38.getString("char_name"));
                        var10.setParameters(new Object[]{var38.getInt("charId"), var38.getInt("class_id")});
                        LOGGER_OLYMPIAD.log(var10);
                        HEROS_TO_BE.add(var38);
                     } else {
                        var38 = NOBLES.get(var38.getInt("charId"));
                        var38.set("charId", var36.getInt("charId"));
                        var27.add(var38);
                     }
                  }
               }
            }

            switch (var27.size()) {
               case 0:
               default:
                  break;
               case 1:
                  StatSet var29 = new StatSet();
                  StatSet var31 = (StatSet)var27.get(0);
                  var29.set("class_id", var31.getInt("class_id"));
                  var29.set("charId", var31.getInt("charId"));
                  var29.set("char_name", var31.getString("char_name"));
                  LogRecord var33 = new LogRecord(Level.INFO, "Hero " + var29.getString("char_name"));
                  var33.setParameters(new Object[]{var29.getInt("charId"), var29.getInt("class_id")});
                  LOGGER_OLYMPIAD.log(var33);
                  HEROS_TO_BE.add(var29);
                  break;
               case 2:
                  StatSet var35 = new StatSet();
                  StatSet var37 = (StatSet)var27.get(0);
                  StatSet var40 = (StatSet)var27.get(1);
                  int var41 = var37.getInt("olympiad_points");
                  int var11 = var40.getInt("olympiad_points");
                  int var12 = var37.getInt("competitions_done");
                  int var13 = var40.getInt("competitions_done");
                  int var14 = var37.getInt("competitions_won");
                  int var15 = var40.getInt("competitions_won");
                  StatSet var16;
                  if (var41 > var11) {
                     var16 = var37;
                  } else if (var11 > var41) {
                     var16 = var40;
                  } else if (var12 > var13) {
                     var16 = var37;
                  } else if (var13 > var12) {
                     var16 = var40;
                  } else if (var14 > var15) {
                     var16 = var37;
                  } else {
                     var16 = var40;
                  }

                  var35.set("class_id", var16.getInt("class_id"));
                  var35.set("charId", var16.getInt("charId"));
                  var35.set("char_name", var16.getString("char_name"));
                  LogRecord var17 = new LogRecord(Level.INFO, "Hero " + var35.getString("char_name"));
                  var17.setParameters(new Object[]{var35.getInt("charId"), var35.getInt("class_id")});
                  LOGGER_OLYMPIAD.log(var17);
                  HEROS_TO_BE.add(var35);
            }
         } catch (SQLException var24) {
            LOGGER.warning("Olympiad System: Could not load heros from DB");
         }
      }
   }

   public List<String> getClassLeaderBoard(int var1) {
      ArrayList var2 = new ArrayList();
      String var3 = OlympiadConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS
         ? (
            var1 == 132
               ? "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = 133) AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10"
               : "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10"
         )
         : (
            var1 == 132
               ? "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = 133) AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10"
               : "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10"
         );

      try (
         Connection var4 = DatabaseFactory.getConnection();
         PreparedStatement var5 = var4.prepareStatement(var3);
      ) {
         var5.setInt(1, var1);

         try (ResultSet var6 = var5.executeQuery()) {
            while (var6.next()) {
               var2.add(var6.getString("char_name"));
            }
         }
      } catch (SQLException var15) {
         LOGGER.warning("Olympiad System: Couldn't load olympiad leaders from DB!");
      }

      return var2;
   }

   private int getNoblessePasses(int var1) {
      if (this._period == 1 && !NOBLES_RANK.isEmpty()) {
         if (!NOBLES_RANK.containsKey(var1)) {
            return 0;
         } else {
            StatSet var2 = NOBLES.get(var1);
            if (var2 != null && var2.getInt("olympiad_points") != 0) {
               int var3 = Hero.getInstance().isHero(var1) ? OlympiadConfig.OLYMPIAD_HERO_POINTS : 0;

               var3 = switch (NOBLES_RANK.get(var1)) {
                  case 1 -> var3 + OlympiadConfig.OLYMPIAD_RANK1_POINTS;
                  case 2 -> var3 + OlympiadConfig.OLYMPIAD_RANK2_POINTS;
                  case 3 -> var3 + OlympiadConfig.OLYMPIAD_RANK3_POINTS;
                  case 4 -> var3 + OlympiadConfig.OLYMPIAD_RANK4_POINTS;
                  default -> var3 + OlympiadConfig.OLYMPIAD_RANK5_POINTS;
               } * OlympiadConfig.OLYMPIAD_GP_PER_POINT;
               var2.set("olympiad_points", 0);
               return var3;
            } else {
               return 0;
            }
         }
      } else {
         return 0;
      }
   }

   public boolean isRegisteredInComp(Player var1) {
      boolean var2 = this.isRegistered(var1);
      if (_inCompPeriod) {
         for (OlympiadGame var4 : OlympiadManager.getInstance().getOlympiadGames().values()) {
            if (var4._playerOneID == var1.getObjectId() || var4._playerTwoID == var1.getObjectId()) {
               var2 = true;
               break;
            }
         }
      }

      return var2;
   }

   public int getNoblePoints(int var1) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet var2 = NOBLES.get(var1);
         return var2 == null ? 0 : var2.getInt("olympiad_points");
      }
   }

   public void setNoblePoints(int var1, int var2) {
      if (!NOBLES.isEmpty()) {
         StatSet var3 = NOBLES.get(var1);
         if (var3 != null) {
            var3.set("olympiad_points", Math.max(0, var2));
         }
      }
   }

   public int getLastNobleOlympiadPoints(int var1) {
      int var2 = 0;

      try (
         Connection var3 = DatabaseFactory.getConnection();
         PreparedStatement var4 = var3.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?");
      ) {
         var4.setInt(1, var1);

         try (ResultSet var5 = var4.executeQuery()) {
            if (var5.first()) {
               var2 = var5.getInt(1);
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Could not load last olympiad points:", (Throwable)var14);
      }

      return var2;
   }

   public int getCompetitionDone(int var1) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet var2 = NOBLES.get(var1);
         return var2 == null ? 0 : var2.getInt("competitions_done");
      }
   }

   public int getCompetitionWon(int var1) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet var2 = NOBLES.get(var1);
         return var2 == null ? 0 : var2.getInt("competitions_won");
      }
   }

   public int getCompetitionLost(int var1) {
      if (NOBLES.isEmpty()) {
         return 0;
      } else {
         StatSet var2 = NOBLES.get(var1);
         return var2 == null ? 0 : var2.getInt("competitions_lost");
      }
   }

   protected void deleteNobles() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("TRUNCATE olympiad_nobles");
      ) {
         var2.execute();
      } catch (SQLException var9) {
         LOGGER.warning("Olympiad System: Couldn't delete nobles from DB!");
      }

      NOBLES.clear();
   }

   public static void sendMatchList(Player var0) {
      NpcHtmlMessage var1 = new NpcHtmlMessage(0);
      var1.setFile(var0, "data/html/olympiad/olympiad_observe2.htm");
      Map var2 = getInstance().getMatchList();

      for (int var3 = 0; var3 < getStadiumCount(); var3++) {
         int var4 = var3 + 1;
         String var5 = "Initial State";
         String var6 = "&nbsp;";
         String var7 = (String)var2.get(var3);
         if (var7 != null) {
            if (OlympiadGame._gameIsStarted) {
               var5 = "Playing";
            } else {
               var5 = "Standby";
            }

            var6 = var7;
         }

         var1.replace("%state" + var4 + "%", var5);
         var1.replace("%players" + var4 + "%", var6);
      }

      var0.sendPacket(var1);
   }

   private static void handleSpectatorEnter(int var0, Player var1) {
      OlympiadGame var2 = OlympiadManager.getInstance().getOlympiadGame(var0);
      if (var2 != null) {
         if (var2._playerOne == null || !var2._playerOne.isInOlympiadMode() || !var2._playerOne.isOlympiadStart()) {
            return;
         }

         if (var2._playerTwo == null || !var2._playerTwo.isInOlympiadMode() || !var2._playerTwo.isOlympiadStart()) {
            return;
         }

         var1.sendPacket(new ExOlympiadUserInfo(var2._playerOne, 1));
         var1.sendPacket(new ExOlympiadUserInfo(var2._playerTwo, 2));
      }
   }

   public static void bypassChangeArena(String var0, Player var1) {
      if (var1.inObserverMode()) {
         String[] var2 = var0.split(" ");
         int var3 = Integer.parseInt(var2[1]);
         int var4 = getSpectatorArena(var1);
         if (var4 >= 0) {
            removeSpectator(var4, var1);
            var1.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
            addSpectator(var3, var1, false);
            handleSpectatorEnter(var3, var1);
         }
      }
   }

   public static org.l2jmobius.gameserver.mechanics.olympiad.Olympiad getInstance() {
      return SingletonHolder._instance;
   }
}
