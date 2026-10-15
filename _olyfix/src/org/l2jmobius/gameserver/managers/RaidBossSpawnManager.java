package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.util.StatSet;

public class RaidBossSpawnManager {
   private static final Logger LOGGER = Logger.getLogger(RaidBossSpawnManager.class.getName());
   protected static final Map<Integer, RaidBoss> _bosses = new ConcurrentHashMap<>();
   protected static final Map<Integer, Spawn> _spawns = new ConcurrentHashMap<>();
   protected static final Map<Integer, StatSet> _storedInfo = new ConcurrentHashMap<>();
   protected static final Map<Integer, ScheduledFuture<?>> _schedules = new ConcurrentHashMap<>();

   protected RaidBossSpawnManager() {
      this.load();
   }

   public void load() {
      if (!_spawns.isEmpty()) {
         for (Spawn spawn : _spawns.values()) {
            this.deleteSpawn(spawn, false);
         }
      }

      _bosses.clear();
      _spawns.clear();
      _storedInfo.clear();
      _schedules.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM raidboss_spawnlist ORDER BY boss_id");
      ) {
         while (rs.next()) {
            Spawn spawnDat = new Spawn(rs.getInt("boss_id"));
            spawnDat.setXYZ(rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
            spawnDat.setAmount(rs.getInt("amount"));
            spawnDat.setHeading(rs.getInt("heading"));
            spawnDat.setRespawnDelay(rs.getInt("respawn_delay"), rs.getInt("respawn_random"));
            this.addNewSpawn(spawnDat, rs.getLong("respawn_time"), rs.getDouble("currentHP"), rs.getDouble("currentMP"), false);
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _bosses.size() + " Instances");
         LOGGER.info(this.getClass().getSimpleName() + ": Scheduled " + _schedules.size() + " Instances");
      } catch (SQLException var12) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not load raidboss_spawnlist table");
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while initializing RaidBossSpawnManager: " + var13.getMessage(), (Throwable)var13);
      }
   }

   public void updateStatus(RaidBoss boss, boolean isBossDead) {
      StatSet info = _storedInfo.get(boss.getId());
      if (info != null) {
         if (isBossDead) {
            boss.setRaidStatus(RaidBossStatus.DEAD);
            int respawnMinDelay = (int)(boss.getSpawn().getRespawnMinDelay() * NpcConfig.RAID_MIN_RESPAWN_MULTIPLIER);
            int respawnMaxDelay = (int)(boss.getSpawn().getRespawnMaxDelay() * NpcConfig.RAID_MAX_RESPAWN_MULTIPLIER);
            int respawnDelay = Rnd.get(respawnMinDelay, respawnMaxDelay);
            long respawnTime = System.currentTimeMillis() + respawnDelay;
            info.set("currentHP", boss.getMaxHp());
            info.set("currentMP", boss.getMaxMp());
            info.set("respawnTime", respawnTime);
            if (!_schedules.containsKey(boss.getId()) && (respawnMinDelay > 0 || respawnMaxDelay > 0)) {
               Calendar time = Calendar.getInstance();
               time.setTimeInMillis(respawnTime);
               LOGGER.info(this.getClass().getSimpleName() + ": Updated " + boss.getName() + " respawn time to " + time.getTime());
               _schedules.put(boss.getId(), ThreadPool.schedule(new RaidBossSpawnManager.SpawnSchedule(boss.getId()), respawnDelay));
               this.updateDb();
            }
         } else {
            boss.setRaidStatus(RaidBossStatus.ALIVE);
            info.set("currentHP", boss.getCurrentHp());
            info.set("currentMP", boss.getCurrentMp());
            info.set("respawnTime", 0);
         }

         _storedInfo.put(boss.getId(), info);
      }
   }

   public void addNewSpawn(Spawn spawnDat, long respawnTime, double currentHP, double currentMP, boolean storeInDb) {
      if (spawnDat != null && !_spawns.containsKey(spawnDat.getId())) {
         int bossId = spawnDat.getId();
         long currentTime = System.currentTimeMillis();
         SpawnTable.getInstance().addSpawn(spawnDat);
         if (respawnTime != 0L && currentTime <= respawnTime) {
            _schedules.put(bossId, ThreadPool.schedule(new RaidBossSpawnManager.SpawnSchedule(bossId), respawnTime - currentTime));
         } else {
            RaidBoss raidboss = (RaidBoss)spawnDat.doSpawn();
            if (raidboss != null) {
               raidboss.setCurrentHp(currentHP);
               raidboss.setCurrentMp(currentMP);
               raidboss.setRaidStatus(RaidBossStatus.ALIVE);
               _bosses.put(bossId, raidboss);
               StatSet info = new StatSet();
               info.set("currentHP", currentHP);
               info.set("currentMP", currentMP);
               info.set("respawnTime", 0);
               _storedInfo.put(bossId, info);
            }
         }

         _spawns.put(bossId, spawnDat);
         if (storeInDb) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement(
                  "INSERT INTO raidboss_spawnlist (boss_id,amount,loc_x,loc_y,loc_z,heading,respawn_time,currentHp,currentMp) VALUES(?,?,?,?,?,?,?,?,?)"
               );
            ) {
               ps.setInt(1, spawnDat.getId());
               ps.setInt(2, spawnDat.getAmount());
               ps.setInt(3, spawnDat.getX());
               ps.setInt(4, spawnDat.getY());
               ps.setInt(5, spawnDat.getZ());
               ps.setInt(6, spawnDat.getHeading());
               ps.setLong(7, respawnTime);
               ps.setDouble(8, currentHP);
               ps.setDouble(9, currentMP);
               ps.execute();
            } catch (Exception var20) {
               LOGGER.log(
                  Level.WARNING,
                  this.getClass().getSimpleName() + ": Could not store raidboss #" + bossId + " in the DB:" + var20.getMessage(),
                  (Throwable)var20
               );
            }
         }
      }
   }

   public void deleteSpawn(Spawn spawnDat, boolean updateDb) {
      if (spawnDat != null) {
         int bossId = spawnDat.getId();
         if (_spawns.containsKey(bossId)) {
            SpawnTable.getInstance().removeSpawn(spawnDat);
            _spawns.remove(bossId);
            if (_bosses.containsKey(bossId)) {
               _bosses.remove(bossId);
            }

            if (_schedules.containsKey(bossId)) {
               _schedules.remove(bossId).cancel(true);
            }

            if (_storedInfo.containsKey(bossId)) {
               _storedInfo.remove(bossId);
            }

            if (updateDb) {
               try (
                  Connection con = DatabaseFactory.getConnection();
                  PreparedStatement ps = con.prepareStatement("DELETE FROM raidboss_spawnlist WHERE boss_id=?");
               ) {
                  ps.setInt(1, bossId);
                  ps.execute();
               } catch (Exception var12) {
                  LOGGER.log(
                     Level.WARNING,
                     this.getClass().getSimpleName() + ": Could not remove raidboss #" + bossId + " from DB: " + var12.getMessage(),
                     (Throwable)var12
                  );
               }
            }
         }
      }
   }

   private void updateDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?");
      ) {
         for (Entry<Integer, StatSet> entry : _storedInfo.entrySet()) {
            Integer bossId = entry.getKey();
            if (bossId != null) {
               RaidBoss boss = _bosses.get(bossId);
               if (boss != null) {
                  if (boss.getRaidStatus() == RaidBossStatus.ALIVE) {
                     this.updateStatus(boss, false);
                  }

                  StatSet info = entry.getValue();
                  if (info != null) {
                     try {
                        ps.setLong(1, info.getLong("respawnTime"));
                        ps.setDouble(2, boss.isDead() ? boss.getMaxHp() : info.getDouble("currentHP"));
                        ps.setDouble(3, boss.isDead() ? boss.getMaxMp() : info.getDouble("currentMP"));
                        ps.setInt(4, bossId);
                        ps.executeUpdate();
                        ps.clearParameters();
                     } catch (SQLException var11) {
                        LOGGER.log(
                           Level.WARNING,
                           this.getClass().getSimpleName() + ": Could not update raidboss_spawnlist table " + var11.getMessage(),
                           (Throwable)var11
                        );
                     }
                  }
               }
            }
         }
      } catch (SQLException var14) {
         LOGGER.log(
            Level.WARNING, this.getClass().getSimpleName() + ": SQL error while updating RaidBoss spawn to database: " + var14.getMessage(), (Throwable)var14
         );
      }
   }

   public String[] getAllRaidBossStatus() {
      String[] msg = new String[_bosses.size()];
      int index = 0;

      for (RaidBoss boss : _bosses.values()) {
         msg[index++] = boss.getName() + ": " + boss.getRaidStatus().name();
      }

      return msg;
   }

   public String getRaidBossStatus(int bossId) {
      String msg = "RaidBoss Status..." + System.lineSeparator();
      RaidBoss boss = _bosses.get(bossId);
      if (boss != null) {
         msg = msg + boss.getName() + ": " + boss.getRaidStatus().name();
      }

      return msg;
   }

   public RaidBossStatus getRaidBossStatusId(int bossId) {
      RaidBoss boss = _bosses.get(bossId);
      if (boss != null) {
         return boss.getRaidStatus();
      } else {
         return _schedules.containsKey(bossId) ? RaidBossStatus.DEAD : RaidBossStatus.UNDEFINED;
      }
   }

   public void notifySpawnNightBoss(RaidBoss raidboss) {
      StatSet info = new StatSet();
      info.set("currentHP", raidboss.getCurrentHp());
      info.set("currentMP", raidboss.getCurrentMp());
      info.set("respawnTime", 0);
      raidboss.setRaidStatus(RaidBossStatus.ALIVE);
      _storedInfo.put(raidboss.getId(), info);
      LOGGER.info(this.getClass().getSimpleName() + ": Spawning Night Raid Boss " + raidboss.getName());
      _bosses.put(raidboss.getId(), raidboss);
   }

   public boolean isDefined(int bossId) {
      return _spawns.containsKey(bossId);
   }

   public Map<Integer, RaidBoss> getBosses() {
      return _bosses;
   }

   public Map<Integer, Spawn> getSpawns() {
      return _spawns;
   }

   public Map<Integer, StatSet> getStoredInfo() {
      return _storedInfo;
   }

   public void cleanUp() {
      this.updateDb();
      _bosses.clear();

      for (ScheduledFuture<?> schedule : _schedules.values()) {
         schedule.cancel(true);
      }

      _schedules.clear();
      _storedInfo.clear();
      _spawns.clear();
   }

   public static RaidBossSpawnManager getInstance() {
      return RaidBossSpawnManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final RaidBossSpawnManager INSTANCE = new RaidBossSpawnManager();
   }

   private static class SpawnSchedule implements Runnable {
      private static final Logger LOGGER = Logger.getLogger(RaidBossSpawnManager.SpawnSchedule.class.getName());
      private final int bossId;

      public SpawnSchedule(int npcId) {
         this.bossId = npcId;
      }

      @Override
      public void run() {
         RaidBoss raidboss = (RaidBoss)RaidBossSpawnManager._spawns.get(this.bossId).doSpawn();
         if (raidboss != null) {
            raidboss.setRaidStatus(RaidBossStatus.ALIVE);
            StatSet info = new StatSet();
            info.set("currentHP", raidboss.getCurrentHp());
            info.set("currentMP", raidboss.getCurrentMp());
            info.set("respawnTime", 0);
            RaidBossSpawnManager._storedInfo.put(this.bossId, info);
            LOGGER.info(this.getClass().getSimpleName() + ": Spawning Raid Boss " + raidboss.getName());
            RaidBossSpawnManager._bosses.put(this.bossId, raidboss);
         }

         RaidBossSpawnManager._schedules.remove(this.bossId);
      }
   }
}
