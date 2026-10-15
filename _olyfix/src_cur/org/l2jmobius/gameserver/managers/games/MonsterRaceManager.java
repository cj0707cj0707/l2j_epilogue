package org.l2jmobius.gameserver.managers.games;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.DerbyTrackZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.MonRaceInfo;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class MonsterRaceManager {
   protected static final Logger LOGGER = Logger.getLogger(MonsterRaceManager.class.getName());
   protected static final PlaySound SOUND_1 = new PlaySound(1, "S_Race", 0, 0, 0, 0, 0);
   protected static final PlaySound SOUND_2 = new PlaySound("ItemSound2.race_start");
   protected static final int[][] CODES = new int[][]{{-1, 0}, {0, 15322}, {13765, -1}};
   protected final List<Integer> _npcTemplates = new ArrayList<>();
   protected final List<MonsterRaceManager.HistoryInfo> _history = new ArrayList<>();
   protected final Map<Integer, Long> _betsPerLane = new ConcurrentHashMap<>();
   protected final List<Double> _odds = new ArrayList<>();
   protected int _raceNumber = 1;
   protected int _finalCountdown = 0;
   protected MonsterRaceManager.RaceState _state = MonsterRaceManager.RaceState.RACE_END;
   protected MonRaceInfo _packet;
   private final Npc[] _monsters = new Npc[8];
   private int[][] _speeds = new int[8][20];
   private int _firstPlace;
   private int _secondPlace;

   protected MonsterRaceManager() {
      if (GeneralConfig.ALLOW_RACE) {
         this.loadHistory();
         this.loadBets();

         for (int i = 31003; i < 31027; i++) {
            this._npcTemplates.add(i);
         }

         ThreadPool.scheduleAtFixedRate(new MonsterRaceManager.Announcement(), 0L, 1000L);
      }
   }

   public void newRace() {
      this._history.add(new MonsterRaceManager.HistoryInfo(this._raceNumber, 0, 0, 0.0));
      Collections.shuffle(this._npcTemplates);

      for (int i = 0; i < 8; i++) {
         try {
            NpcTemplate template = NpcData.getInstance().getTemplate(this._npcTemplates.get(i));
            this._monsters[i] = (Npc)Class.forName("org.l2jmobius.gameserver.entity.actor.instance." + template.getType()).getConstructors()[0]
               .newInstance(template);
         } catch (Exception var3) {
            LOGGER.log(Level.WARNING, "newRace: ", (Throwable)var3);
         }
      }
   }

   public void newSpeeds() {
      this._speeds = new int[8][20];
      int total = 0;
      int winnerDistance = 0;
      int secondDistance = 0;

      for (int i = 0; i < 8; i++) {
         total = 0;

         for (int j = 0; j < 20; j++) {
            if (j == 19) {
               this._speeds[i][j] = 100;
            } else {
               this._speeds[i][j] = Rnd.get(60) + 65;
            }

            total += this._speeds[i][j];
         }

         if (total >= winnerDistance) {
            this._secondPlace = this._firstPlace;
            secondDistance = winnerDistance;
            this._firstPlace = i;
            winnerDistance = total;
         } else if (total >= secondDistance) {
            this._secondPlace = i;
            secondDistance = total;
         }
      }
   }

   protected void loadHistory() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT * FROM mdt_history");
         ResultSet rset = statement.executeQuery();
      ) {
         while (rset.next()) {
            this._history
               .add(new MonsterRaceManager.HistoryInfo(rset.getInt("race_id"), rset.getInt("first"), rset.getInt("second"), rset.getDouble("odd_rate")));
            this._raceNumber++;
         }
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, "MonsterRace: Can't load history: " + var12.getMessage(), (Throwable)var12);
      }

      LOGGER.info("MonsterRace: loaded " + this._history.size() + " records, currently on race #" + this._raceNumber);
   }

   protected void saveHistory(MonsterRaceManager.HistoryInfo history) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("REPLACE INTO mdt_history (race_id, first, second, odd_rate) VALUES (?,?,?,?)");
      ) {
         statement.setInt(1, history.getRaceId());
         statement.setInt(2, history.getFirst());
         statement.setInt(3, history.getSecond());
         statement.setDouble(4, history.getOddRate());
         statement.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, "MonsterRace: Can't save history: " + var10.getMessage(), (Throwable)var10);
      }
   }

   protected void loadBets() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT * FROM mdt_bets");
         ResultSet rset = statement.executeQuery();
      ) {
         while (rset.next()) {
            this.setBetOnLane(rset.getInt("lane_id"), rset.getLong("bet"), false);
         }
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, "MonsterRace: Can't load bets: " + var12.getMessage(), (Throwable)var12);
      }
   }

   protected void saveBet(int lane, long sum) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("REPLACE INTO mdt_bets (lane_id, bet) VALUES (?,?)");
      ) {
         statement.setInt(1, lane);
         statement.setLong(2, sum);
         statement.execute();
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, "MonsterRace: Can't save bet: " + var12.getMessage(), (Throwable)var12);
      }
   }

   protected void clearBets() {
      for (int key : this._betsPerLane.keySet()) {
         this._betsPerLane.put(key, 0L);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("UPDATE mdt_bets SET bet = 0");
      ) {
         statement.execute();
      } catch (SQLException var9) {
         LOGGER.log(Level.WARNING, "MonsterRace: Can't clear bets: " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void setBetOnLane(int lane, long amount, boolean saveOnDb) {
      long sum = this._betsPerLane.containsKey(lane) ? this._betsPerLane.get(lane) + amount : amount;
      this._betsPerLane.put(lane, sum);
      if (saveOnDb) {
         this.saveBet(lane, sum);
      }
   }

   protected void calculateOdds() {
      this._odds.clear();
      Map<Integer, Long> sortedLanes = new TreeMap<>(this._betsPerLane);
      long sumOfAllLanes = 0L;

      for (long amount : sortedLanes.values()) {
         sumOfAllLanes += amount;
      }

      for (long amount : sortedLanes.values()) {
         this._odds.add(amount == 0L ? 0.0 : Math.max(1.25, sumOfAllLanes * 0.7 / amount));
      }
   }

   private <T extends ZoneType> void broadcastToAllPlayersInDerbyZone(ServerPacket... packets) {
      for (ServerPacket packet : packets) {
         packet.sendInBroadcast();
      }

      for (ZoneType zone : ZoneManager.getInstance().getAllZones(DerbyTrackZone.class)) {
         for (Creature creature : zone.getCharactersInside()) {
            if (creature != null) {
               for (ServerPacket packet : packets) {
                  creature.sendPacket(packet);
               }
            }
         }
      }
   }

   public Npc[] getMonsters() {
      return this._monsters;
   }

   public int[][] getSpeeds() {
      return this._speeds;
   }

   public int getFirstPlace() {
      return this._firstPlace;
   }

   public int getSecondPlace() {
      return this._secondPlace;
   }

   public MonRaceInfo getRacePacket() {
      return this._packet;
   }

   public MonsterRaceManager.RaceState getCurrentRaceState() {
      return this._state;
   }

   public int getRaceNumber() {
      return this._raceNumber;
   }

   public List<MonsterRaceManager.HistoryInfo> getHistory() {
      return this._history;
   }

   public List<Double> getOdds() {
      return this._odds;
   }

   public static MonsterRaceManager getInstance() {
      return MonsterRaceManager.SingletonHolder.INSTANCE;
   }

   private class Announcement implements Runnable {
      public Announcement() {
         Objects.requireNonNull(MonsterRaceManager.this);
         super();
      }

      @Override
      public void run() {
         if (MonsterRaceManager.this._finalCountdown > 1200) {
            MonsterRaceManager.this._finalCountdown = 0;
         }

         switch (MonsterRaceManager.this._finalCountdown) {
            case 0: {
               MonsterRaceManager.this.newRace();
               MonsterRaceManager.this.newSpeeds();
               MonsterRaceManager.this._state = MonsterRaceManager.RaceState.ACCEPTING_BETS;
               MonsterRaceManager.this._packet = new MonRaceInfo(
                  MonsterRaceManager.CODES[0][0], MonsterRaceManager.CODES[0][1], MonsterRaceManager.this.getMonsters(), MonsterRaceManager.this.getSpeeds()
               );
               SystemMessage msg = new SystemMessage(SystemMessageId.TICKETS_ARE_NOW_AVAILABLE_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(MonsterRaceManager.this._packet, msg);
               break;
            }
            case 30:
            case 60:
            case 90:
            case 120:
            case 150:
            case 180:
            case 210:
            case 240:
            case 270:
            case 330:
            case 360:
            case 390:
            case 420:
            case 450:
            case 480:
            case 510:
            case 540:
            case 570:
            case 630:
            case 660:
            case 690:
            case 720:
            case 750:
            case 780:
            case 810:
            case 870: {
               SystemMessage msg = new SystemMessage(SystemMessageId.TICKETS_ARE_NOW_AVAILABLE_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg);
               break;
            }
            case 300: {
               SystemMessage msg = new SystemMessage(SystemMessageId.NOW_SELLING_TICKETS_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               SystemMessage msg2 = new SystemMessage(SystemMessageId.TICKET_SALES_FOR_THE_MONSTER_RACE_WILL_END_IN_S1_MINUTE_S);
               msg2.addInt(10);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg, msg2);
               break;
            }
            case 600: {
               SystemMessage msg = new SystemMessage(SystemMessageId.NOW_SELLING_TICKETS_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               SystemMessage msg2 = new SystemMessage(SystemMessageId.TICKET_SALES_FOR_THE_MONSTER_RACE_WILL_END_IN_S1_MINUTE_S);
               msg2.addInt(5);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg, msg2);
               break;
            }
            case 840: {
               SystemMessage msg = new SystemMessage(SystemMessageId.NOW_SELLING_TICKETS_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               SystemMessage msg2 = new SystemMessage(SystemMessageId.TICKET_SALES_FOR_THE_MONSTER_RACE_WILL_END_IN_S1_MINUTE_S);
               msg2.addInt(1);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg, msg2);
               break;
            }
            case 900: {
               MonsterRaceManager.this._state = MonsterRaceManager.RaceState.WAITING;
               MonsterRaceManager.this.calculateOdds();
               SystemMessage msg = new SystemMessage(SystemMessageId.TICKETS_ARE_NOW_AVAILABLE_FOR_MONSTER_RACE_S1);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               SystemMessage msg2 = new SystemMessage(SystemMessageId.TICKETS_SALES_ARE_CLOSED_FOR_MONSTER_RACE_S1_ODDS_ARE_POSTED);
               msg2.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg, msg2);
               break;
            }
            case 960:
            case 1020: {
               int minutes = MonsterRaceManager.this._finalCountdown == 960 ? 2 : 1;
               SystemMessage msg = new SystemMessage(SystemMessageId.MONSTER_RACE_S2_WILL_BEGIN_IN_S1_MINUTE_S);
               msg.addInt(minutes);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg);
               break;
            }
            case 1050: {
               SystemMessage msg = new SystemMessage(SystemMessageId.MONSTER_RACE_S1_WILL_BEGIN_IN_30_SECONDS);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg);
               break;
            }
            case 1070: {
               SystemMessage msg = new SystemMessage(SystemMessageId.MONSTER_RACE_S1_IS_ABOUT_TO_BEGIN_COUNTDOWN_IN_FIVE_SECONDS);
               msg.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg);
               break;
            }
            case 1075:
            case 1076:
            case 1077:
            case 1078:
            case 1079: {
               int seconds = 1080 - MonsterRaceManager.this._finalCountdown;
               SystemMessage msg = new SystemMessage(SystemMessageId.THE_RACE_WILL_BEGIN_IN_S1_SECOND_S);
               msg.addInt(seconds);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg);
               break;
            }
            case 1080:
               MonsterRaceManager.this._state = MonsterRaceManager.RaceState.STARTING_RACE;
               MonsterRaceManager.this._packet = new MonRaceInfo(
                  MonsterRaceManager.CODES[1][0], MonsterRaceManager.CODES[1][1], MonsterRaceManager.this.getMonsters(), MonsterRaceManager.this.getSpeeds()
               );
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(
                  new SystemMessage(SystemMessageId.THEY_RE_OFF), MonsterRaceManager.SOUND_1, MonsterRaceManager.SOUND_2, MonsterRaceManager.this._packet
               );
               break;
            case 1085:
               MonsterRaceManager.this._packet = new MonRaceInfo(
                  MonsterRaceManager.CODES[2][0], MonsterRaceManager.CODES[2][1], MonsterRaceManager.this.getMonsters(), MonsterRaceManager.this.getSpeeds()
               );
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(MonsterRaceManager.this._packet);
               break;
            case 1115: {
               MonsterRaceManager.this._state = MonsterRaceManager.RaceState.RACE_END;
               MonsterRaceManager.HistoryInfo info = MonsterRaceManager.this._history.get(MonsterRaceManager.this._history.size() - 1);
               info.setFirst(MonsterRaceManager.this.getFirstPlace());
               info.setSecond(MonsterRaceManager.this.getSecondPlace());
               info.setOddRate(MonsterRaceManager.this._odds.get(MonsterRaceManager.this.getFirstPlace()));
               MonsterRaceManager.this.saveHistory(info);
               MonsterRaceManager.this.clearBets();
               SystemMessage msg = new SystemMessage(SystemMessageId.FIRST_PRIZE_GOES_TO_THE_PLAYER_IN_LANE_S1_SECOND_PRIZE_GOES_TO_THE_PLAYER_IN_LANE_S2);
               msg.addInt(MonsterRaceManager.this.getFirstPlace() + 1);
               msg.addInt(MonsterRaceManager.this.getSecondPlace() + 1);
               SystemMessage msg2 = new SystemMessage(SystemMessageId.MONSTER_RACE_S1_IS_FINISHED);
               msg2.addInt(MonsterRaceManager.this._raceNumber);
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(msg, msg2);
               MonsterRaceManager.this._raceNumber++;
               break;
            }
            case 1140:
               MonsterRaceManager.this.broadcastToAllPlayersInDerbyZone(
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[0]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[1]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[2]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[3]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[4]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[5]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[6]),
                  new DeleteObject(MonsterRaceManager.this.getMonsters()[7])
               );
         }

         MonsterRaceManager.this._finalCountdown++;
      }
   }

   public static class HistoryInfo {
      private final int _raceId;
      private int _first;
      private int _second;
      private double _oddRate;

      public HistoryInfo(int raceId, int first, int second, double oddRate) {
         this._raceId = raceId;
         this._first = first;
         this._second = second;
         this._oddRate = oddRate;
      }

      public int getRaceId() {
         return this._raceId;
      }

      public int getFirst() {
         return this._first;
      }

      public int getSecond() {
         return this._second;
      }

      public double getOddRate() {
         return this._oddRate;
      }

      public void setFirst(int first) {
         this._first = first;
      }

      public void setSecond(int second) {
         this._second = second;
      }

      public void setOddRate(double oddRate) {
         this._oddRate = oddRate;
      }
   }

   public static enum RaceState {
      ACCEPTING_BETS,
      WAITING,
      STARTING_RACE,
      RACE_END;
   }

   private static class SingletonHolder {
      protected static final MonsterRaceManager INSTANCE = new MonsterRaceManager();
   }
}
