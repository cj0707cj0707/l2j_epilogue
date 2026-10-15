package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;

public class RaidBossPointsManager {
   private static final Logger LOGGER = Logger.getLogger(RaidBossPointsManager.class.getName());
   private final Map<Integer, Map<Integer, Integer>> _list = new ConcurrentHashMap<>();

   public RaidBossPointsManager() {
      this.init();
      ThreadPool.scheduleAtFixedRate(this::resetRaidPoints, TimeUtil.getNextTime(0, 10).getTimeInMillis() - System.currentTimeMillis(), 86400000L);
   }

   private void init() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT `charId`,`boss_id`,`points` FROM `character_raid_points`");
      ) {
         while (rs.next()) {
            int charId = rs.getInt("charId");
            int bossId = rs.getInt("boss_id");
            int points = rs.getInt("points");
            Map<Integer, Integer> values = this._list.get(charId);
            if (values == null) {
               values = new HashMap<>();
            }

            values.put(bossId, points);
            this._list.put(charId, values);
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._list.size() + " Characters Raid Points.");
      } catch (SQLException var14) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load raid points ", (Throwable)var14);
      }
   }

   private void resetRaidPoints() {
      Calendar calendar = Calendar.getInstance();
      if (calendar.get(7) == 2) {
         Map<Integer, Integer> rankList = getInstance().getRankList();

         for (Clan clan : ClanTable.getInstance().getClans()) {
            for (Entry<Integer, Integer> entry : rankList.entrySet()) {
               if (entry.getValue() <= 100 && clan.isMember(entry.getKey())) {
                  int reputation = 0;

                  clan.addReputationScore(switch (entry.getValue()) {
                     case 1 -> FeatureConfig.RAID_RANKING_1ST;
                     case 2 -> FeatureConfig.RAID_RANKING_2ND;
                     case 3 -> FeatureConfig.RAID_RANKING_3RD;
                     case 4 -> FeatureConfig.RAID_RANKING_4TH;
                     case 5 -> FeatureConfig.RAID_RANKING_5TH;
                     case 6 -> FeatureConfig.RAID_RANKING_6TH;
                     case 7 -> FeatureConfig.RAID_RANKING_7TH;
                     case 8 -> FeatureConfig.RAID_RANKING_8TH;
                     case 9 -> FeatureConfig.RAID_RANKING_9TH;
                     case 10 -> FeatureConfig.RAID_RANKING_10TH;
                     default -> {
                        if (entry.getValue() <= 50) {
                           yield FeatureConfig.RAID_RANKING_UP_TO_50TH;
                        } else {
                           yield FeatureConfig.RAID_RANKING_UP_TO_100TH;
                        }
                     }
                  });
               }
            }
         }

         this.cleanUp();
         LOGGER.info(this.getClass().getSimpleName() + ": Reset task launched.");
      }
   }

   public void updatePointsInDB(Player player, int raidId, int points) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("REPLACE INTO character_raid_points (`charId`,`boss_id`,`points`) VALUES (?,?,?)");
      ) {
         ps.setInt(1, player.getObjectId());
         ps.setInt(2, raidId);
         ps.setInt(3, points);
         ps.executeUpdate();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't update char raid points for player: " + player, (Throwable)var12);
      }
   }

   public void addPoints(Player player, int bossId, int points) {
      Map<Integer, Integer> tmpPoint = this._list.computeIfAbsent(player.getObjectId(), var0 -> new HashMap<>());
      this.updatePointsInDB(player, bossId, tmpPoint.merge(bossId, points, Integer::sum));
   }

   public int getPointsByOwnerId(int ownerId) {
      Map<Integer, Integer> tmpPoint = this._list.get(ownerId);
      int totalPoints = 0;
      if (tmpPoint != null && !tmpPoint.isEmpty()) {
         for (int points : tmpPoint.values()) {
            totalPoints += points;
         }

         return totalPoints;
      } else {
         return 0;
      }
   }

   public Map<Integer, Integer> getList(Player player) {
      return this._list.get(player.getObjectId());
   }

   public void cleanUp() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0");
      ) {
         statement.executeUpdate();
         this._list.clear();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't clean raid points", (Throwable)var9);
      }
   }

   public int calculateRanking(int playerObjId) {
      return this.getRankList().getOrDefault(playerObjId, 0);
   }

   public Map<Integer, Integer> getRankList() {
      Map<Integer, Integer> tmpPoints = new HashMap<>();

      for (int ownerId : this._list.keySet()) {
         int totalPoints = this.getPointsByOwnerId(ownerId);
         if (totalPoints != 0) {
            tmpPoints.put(ownerId, totalPoints);
         }
      }

      List<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());
      list.sort(Comparator.comparingInt(Entry::getValue).reversed());
      int ranking = 1;
      Map<Integer, Integer> tmpRanking = new HashMap<>();

      for (Entry<Integer, Integer> entry : list) {
         tmpRanking.put(entry.getKey(), ranking++);
      }

      return tmpRanking;
   }

   public static RaidBossPointsManager getInstance() {
      return RaidBossPointsManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final RaidBossPointsManager INSTANCE = new RaidBossPointsManager();
   }
}
