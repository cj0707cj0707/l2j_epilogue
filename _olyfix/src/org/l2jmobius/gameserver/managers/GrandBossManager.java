package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.zone.type.BossZone;
import org.l2jmobius.gameserver.util.StatSet;

public class GrandBossManager {
   private static final String DELETE_GRAND_BOSS_LIST = "DELETE FROM grandboss_list";
   private static final String INSERT_GRAND_BOSS_LIST = "INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)";
   private static final String UPDATE_GRAND_BOSS_DATA = "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?";
   private static final String UPDATE_GRAND_BOSS_DATA2 = "UPDATE grandboss_data set status = ? where boss_id = ?";
   protected static final Logger LOGGER = Logger.getLogger(GrandBossManager.class.getName());
   protected static final Map<Integer, GrandBoss> BOSSES = new ConcurrentHashMap<>();
   protected static Map<Integer, StatSet> _storedInfo = new HashMap<>();
   private final Map<Integer, Integer> _bossStatus = new ConcurrentHashMap<>();
   private final Map<Integer, BossZone> _zones = new ConcurrentHashMap<>();

   protected GrandBossManager() {
      this.init();
   }

   private void init() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * from grandboss_data ORDER BY boss_id");
      ) {
         while (rs.next()) {
            StatSet info = new StatSet();
            int bossId = rs.getInt("boss_id");
            info.set("loc_x", rs.getInt("loc_x"));
            info.set("loc_y", rs.getInt("loc_y"));
            info.set("loc_z", rs.getInt("loc_z"));
            info.set("heading", rs.getInt("heading"));
            info.set("respawn_time", rs.getLong("respawn_time"));
            info.set("currentHP", rs.getDouble("currentHP"));
            info.set("currentMP", rs.getDouble("currentMP"));
            int status = rs.getInt("status");
            this._bossStatus.put(bossId, status);
            _storedInfo.put(bossId, info);
            LOGGER.info(
               this.getClass().getSimpleName() + ": " + NpcData.getInstance().getTemplate(bossId).getName() + "(" + bossId + ") status is " + status + "."
            );
            if (status > 0) {
               LOGGER.info(
                  this.getClass().getSimpleName()
                     + ": Next spawn date of "
                     + NpcData.getInstance().getTemplate(bossId).getName()
                     + " is "
                     + new Date(info.getLong("respawn_time"))
                     + "."
               );
            }
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _storedInfo.size() + " Instances");
      } catch (SQLException var13) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load grandboss_data table: " + var13.getMessage(), (Throwable)var13);
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Error while initializing GrandBossManager: " + var14.getMessage(), (Throwable)var14);
      }

      ThreadPool.scheduleAtFixedRate(this::storeMe, 300000L, 300000L);
   }

   public void initZones() {
      Map<Integer, List<Integer>> zones = new HashMap<>();

      for (Integer zoneId : this._zones.keySet()) {
         zones.put(zoneId, new ArrayList<>());
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * from grandboss_list ORDER BY player_id");
      ) {
         while (rs.next()) {
            zones.get(rs.getInt("zone")).add(rs.getInt("player_id"));
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Initialized " + this._zones.size() + " Grand Boss Zones");
      } catch (SQLException var13) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load grandboss_list table: " + var13.getMessage(), (Throwable)var13);
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Error while initializing GrandBoss zones: " + var14.getMessage(), (Throwable)var14);
      }

      for (Entry<Integer, BossZone> e : this._zones.entrySet()) {
         e.getValue().setAllowedPlayers(zones.get(e.getKey()));
      }

      zones.clear();
   }

   public void addZone(BossZone zone) {
      this._zones.put(zone.getId(), zone);
   }

   public BossZone getZone(int zoneId) {
      return this._zones.get(zoneId);
   }

   public BossZone getZone(Creature creature) {
      for (BossZone zone : this._zones.values()) {
         if (zone.isCharacterInZone(creature)) {
            return zone;
         }
      }

      return null;
   }

   public BossZone getZone(Location loc) {
      return this.getZone(loc.getX(), loc.getY(), loc.getZ());
   }

   public BossZone getZone(int x, int y, int z) {
      for (BossZone zone : this._zones.values()) {
         if (zone.isInsideZone(x, y, z)) {
            return zone;
         }
      }

      return null;
   }

   public boolean checkIfInZone(String zoneType, WorldObject obj) {
      BossZone temp = this.getZone(obj.getX(), obj.getY(), obj.getZ());
      return temp != null && temp.getName().equalsIgnoreCase(zoneType);
   }

   public boolean checkIfInZone(Player player) {
      return player != null && this.getZone(player.getX(), player.getY(), player.getZ()) != null;
   }

   public int getStatus(int bossId) {
      return this._bossStatus.get(bossId);
   }

   public void setStatus(int bossId, int status) {
      this._bossStatus.put(bossId, status);
      LOGGER.info(this.getClass().getSimpleName() + ": Updated " + NpcData.getInstance().getTemplate(bossId).getName() + "(" + bossId + ") status to " + status);
      this.updateDb(bossId, true);
   }

   public void addBoss(GrandBoss boss) {
      if (boss != null) {
         BOSSES.put(boss.getId(), boss);
      }
   }

   public GrandBoss getBoss(int bossId) {
      return BOSSES.get(bossId);
   }

   public StatSet getStatSet(int bossId) {
      return _storedInfo.get(bossId);
   }

   public void setStatSet(int bossId, StatSet info) {
      _storedInfo.put(bossId, info);
      this.updateDb(bossId, false);
   }

   public boolean storeMe() {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement s = con.createStatement();
         ) {
            s.executeUpdate("DELETE FROM grandboss_list");

            try (PreparedStatement insert = con.prepareStatement("INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)")) {
               for (Entry<Integer, BossZone> e : this._zones.entrySet()) {
                  List<Integer> list = e.getValue().getAllowedPlayers();
                  if (list != null && !list.isEmpty()) {
                     for (Integer player : list) {
                        insert.setInt(1, player);
                        insert.setInt(2, e.getKey());
                        insert.executeUpdate();
                        insert.clearParameters();
                     }
                  }
               }
            }

            for (Entry<Integer, StatSet> ex : _storedInfo.entrySet()) {
               GrandBoss boss = BOSSES.get(ex.getKey());
               StatSet info = ex.getValue();
               if (boss != null && info != null) {
                  try (PreparedStatement update = con.prepareStatement(
                        "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?"
                     )) {
                     update.setInt(1, boss.getX());
                     update.setInt(2, boss.getY());
                     update.setInt(3, boss.getZ());
                     update.setInt(4, boss.getHeading());
                     update.setLong(5, info.getLong("respawn_time"));
                     double hp = boss.getCurrentHp();
                     double mp = boss.getCurrentMp();
                     if (boss.isDead()) {
                        hp = boss.getMaxHp();
                        mp = boss.getMaxMp();
                     }

                     update.setDouble(6, hp);
                     update.setDouble(7, mp);
                     update.setInt(8, this._bossStatus.get(ex.getKey()));
                     update.setInt(9, ex.getKey());
                     update.executeUpdate();
                     update.clearParameters();
                  }
               } else {
                  try (PreparedStatement update = con.prepareStatement("UPDATE grandboss_data set status = ? where boss_id = ?")) {
                     update.setInt(1, this._bossStatus.get(ex.getKey()));
                     update.setInt(2, ex.getKey());
                     update.executeUpdate();
                     update.clearParameters();
                  }
               }
            }
         }

         return true;
      } catch (SQLException var22) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't store grandbosses to database:" + var22.getMessage(), (Throwable)var22);
         return false;
      }
   }

   private void updateDb(int bossId, boolean statusOnly) {
      try (Connection con = DatabaseFactory.getConnection()) {
         GrandBoss boss = BOSSES.get(bossId);
         StatSet info = _storedInfo.get(bossId);
         if (!statusOnly && boss != null && info != null) {
            try (PreparedStatement ps = con.prepareStatement(
                  "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?"
               )) {
               ps.setInt(1, boss.getX());
               ps.setInt(2, boss.getY());
               ps.setInt(3, boss.getZ());
               ps.setInt(4, boss.getHeading());
               ps.setLong(5, info.getLong("respawn_time"));
               double hp = boss.getCurrentHp();
               double mp = boss.getCurrentMp();
               if (boss.isDead()) {
                  hp = boss.getMaxHp();
                  mp = boss.getMaxMp();
               }

               ps.setDouble(6, hp);
               ps.setDouble(7, mp);
               ps.setInt(8, this._bossStatus.get(bossId));
               ps.setInt(9, bossId);
               ps.executeUpdate();
            }
         } else {
            try (PreparedStatement ps = con.prepareStatement("UPDATE grandboss_data set status = ? where boss_id = ?")) {
               ps.setInt(1, this._bossStatus.get(bossId));
               ps.setInt(2, bossId);
               ps.executeUpdate();
            }
         }
      } catch (SQLException var17) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't update grandbosses to database:" + var17.getMessage(), (Throwable)var17);
      }
   }

   public void cleanUp() {
      this.storeMe();
      BOSSES.clear();
      _storedInfo.clear();
      this._bossStatus.clear();
      this._zones.clear();
   }

   public Map<Integer, BossZone> getZones() {
      return this._zones;
   }

   public static GrandBossManager getInstance() {
      return GrandBossManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final GrandBossManager INSTANCE = new GrandBossManager();
   }
}
