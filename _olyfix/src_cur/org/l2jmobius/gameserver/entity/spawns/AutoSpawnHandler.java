package org.l2jmobius.gameserver.entity.spawns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.managers.IdManager;

public class AutoSpawnHandler {
   protected static final Logger LOGGER = Logger.getLogger(AutoSpawnHandler.class.getName());
   private static final int DEFAULT_INITIAL_SPAWN = 30000;
   private static final int DEFAULT_RESPAWN = 3600000;
   private static final int DEFAULT_DESPAWN = 3600000;
   protected Map<Integer, AutoSpawnHandler.AutoSpawnInstance> _registeredSpawns = new ConcurrentHashMap<>();
   protected Map<Integer, ScheduledFuture<?>> _runningSpawns = new ConcurrentHashMap<>();
   protected boolean _activeState = true;

   protected AutoSpawnHandler() {
      this.restoreSpawnData();
   }

   public int size() {
      return this._registeredSpawns.size();
   }

   public void reload() {
      this.unload();
      this._registeredSpawns.clear();
      this._runningSpawns.clear();
      this.restoreSpawnData();
   }

   public void unload() {
      for (ScheduledFuture<?> sf : this._runningSpawns.values()) {
         if (sf != null) {
            sf.cancel(true);
         }
      }

      for (AutoSpawnHandler.AutoSpawnInstance asi : this._registeredSpawns.values()) {
         if (asi != null) {
            this.removeSpawn(asi);
         }
      }
   }

   private void restoreSpawnData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM random_spawn ORDER BY groupId ASC");
         PreparedStatement ps = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?");
      ) {
         while (rs.next()) {
            AutoSpawnHandler.AutoSpawnInstance spawnInst = this.registerSpawn(
               rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay")
            );
            spawnInst.setSpawnCount(rs.getInt("count"));
            spawnInst.setBroadcast(Boolean.parseBoolean(rs.getString("broadcastSpawn")));
            spawnInst.setRandomSpawn(Boolean.parseBoolean(rs.getString("randomSpawn")));
            ps.setInt(1, rs.getInt("groupId"));

            try (ResultSet rs2 = ps.executeQuery()) {
               ps.clearParameters();

               while (rs2.next()) {
                  spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
               }
            }
         }
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, "AutoSpawnHandler: Could not restore spawn data: " + var19.getMessage(), (Throwable)var19);
      }
   }

   public AutoSpawnHandler.AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay) {
      AutoSpawnHandler.AutoSpawnInstance newSpawn = new AutoSpawnHandler.AutoSpawnInstance(
         npcId, initialDelay < 0 ? 30000 : initialDelay, respawnDelay < 0 ? 3600000 : respawnDelay, despawnDelay < 0 ? 3600000 : despawnDelay
      );
      if (spawnPoints != null) {
         for (int[] spawnPoint : spawnPoints) {
            newSpawn.addSpawnLocation(spawnPoint);
         }
      }

      int newId = IdManager.getInstance().getNextId();
      newSpawn._objectId = newId;
      this._registeredSpawns.put(newId, newSpawn);
      this.setSpawnActive(newSpawn, true);
      return newSpawn;
   }

   public AutoSpawnHandler.AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay) {
      return this.registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
   }

   public boolean removeSpawn(AutoSpawnHandler.AutoSpawnInstance spawnInst) {
      if (!this.isSpawnRegistered(spawnInst)) {
         return false;
      } else {
         try {
            this._registeredSpawns.remove(spawnInst.getId());
            ScheduledFuture<?> task = this._runningSpawns.remove(spawnInst._objectId);
            if (task != null) {
               task.cancel(false);
            }

            return true;
         } catch (Exception var3) {
            LOGGER.log(
               Level.WARNING,
               "AutoSpawnHandler: Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + "): " + var3.getMessage(),
               (Throwable)var3
            );
            return false;
         }
      }
   }

   public void removeSpawn(int objectId) {
      this.removeSpawn(this._registeredSpawns.get(objectId));
   }

   public void setSpawnActive(AutoSpawnHandler.AutoSpawnInstance spawnInst, boolean isActive) {
      if (spawnInst != null) {
         int objectId = spawnInst._objectId;
         if (this.isSpawnRegistered(objectId)) {
            ScheduledFuture<?> spawnTask = null;
            if (isActive) {
               AutoSpawnHandler.AutoSpawner rs = new AutoSpawnHandler.AutoSpawner(objectId);
               spawnTask = spawnInst._desDelay > 0
                  ? ThreadPool.scheduleAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay)
                  : ThreadPool.schedule(rs, spawnInst._initDelay);
               this._runningSpawns.put(objectId, spawnTask);
            } else {
               AutoSpawnHandler.AutoDespawner rd = new AutoSpawnHandler.AutoDespawner(objectId);
               spawnTask = this._runningSpawns.remove(objectId);
               if (spawnTask != null) {
                  spawnTask.cancel(false);
               }

               ThreadPool.schedule(rd, 0L);
            }

            spawnInst.setSpawnActive(isActive);
         }
      }
   }

   public void setAllActive(boolean isActive) {
      if (this._activeState != isActive) {
         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : this._registeredSpawns.values()) {
            this.setSpawnActive(spawnInst, isActive);
         }

         this._activeState = isActive;
      }
   }

   public long getTimeToNextSpawn(AutoSpawnHandler.AutoSpawnInstance spawnInst) {
      int objectId = spawnInst.getObjectId();
      return !this.isSpawnRegistered(objectId)
         ? -1L
         : (this._runningSpawns.containsKey(objectId) ? this._runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS) : 0L);
   }

   public AutoSpawnHandler.AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId) {
      if (isObjectId) {
         if (this.isSpawnRegistered(id)) {
            return this._registeredSpawns.get(id);
         }
      } else {
         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : this._registeredSpawns.values()) {
            if (spawnInst.getId() == id) {
               return spawnInst;
            }
         }
      }

      return null;
   }

   public List<AutoSpawnHandler.AutoSpawnInstance> getAutoSpawnInstances(int npcId) {
      List<AutoSpawnHandler.AutoSpawnInstance> result = new ArrayList<>();

      for (AutoSpawnHandler.AutoSpawnInstance spawnInst : this._registeredSpawns.values()) {
         if (spawnInst.getId() == npcId) {
            result.add(spawnInst);
         }
      }

      return result;
   }

   public boolean isSpawnRegistered(int objectId) {
      return this._registeredSpawns.containsKey(objectId);
   }

   public boolean isSpawnRegistered(AutoSpawnHandler.AutoSpawnInstance spawnInst) {
      return this._registeredSpawns.containsValue(spawnInst);
   }

   public static AutoSpawnHandler getInstance() {
      return AutoSpawnHandler.SingletonHolder.INSTANCE;
   }

   private class AutoDespawner implements Runnable {
      private final int _objectId;

      protected AutoDespawner(int objectId) {
         Objects.requireNonNull(AutoSpawnHandler.this);
         super();
         this._objectId = objectId;
      }

      @Override
      public void run() {
         try {
            AutoSpawnHandler.AutoSpawnInstance spawnInst = AutoSpawnHandler.this._registeredSpawns.get(this._objectId);
            if (spawnInst == null) {
               AutoSpawnHandler.LOGGER.info("AutoSpawnHandler: No spawn registered for object ID = " + this._objectId + ".");
               return;
            }

            for (Npc npcInst : spawnInst.getNpcList()) {
               if (npcInst != null) {
                  npcInst.deleteMe();
                  SpawnTable.getInstance().removeSpawn(npcInst.getSpawn());
                  spawnInst.removeNpc(npcInst);
               }
            }
         } catch (Exception var4) {
            AutoSpawnHandler.LOGGER
               .log(
                  Level.WARNING,
                  "AutoSpawnHandler: An error occurred while despawning spawn (Object ID = " + this._objectId + "): " + var4.getMessage(),
                  (Throwable)var4
               );
         }
      }
   }

   public static class AutoSpawnInstance {
      protected int _objectId;
      protected int _spawnIndex;
      protected int _npcId;
      protected int _initDelay;
      protected int _resDelay;
      protected int _desDelay;
      protected int _spawnCount = 1;
      protected int _lastLocIndex = -1;
      private final Queue<Npc> _npcList = new ConcurrentLinkedQueue<>();
      private final List<Location> _locList = new CopyOnWriteArrayList<>();
      private boolean _spawnActive;
      private boolean _randomSpawn = false;
      private boolean _broadcastAnnouncement = false;

      protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay) {
         this._npcId = npcId;
         this._initDelay = initDelay;
         this._resDelay = respawnDelay;
         this._desDelay = despawnDelay;
      }

      protected void setSpawnActive(boolean activeValue) {
         this._spawnActive = activeValue;
      }

      protected boolean addNpc(Npc npcInst) {
         return this._npcList.add(npcInst);
      }

      protected boolean removeNpc(Npc npcInst) {
         return this._npcList.remove(npcInst);
      }

      public int getObjectId() {
         return this._objectId;
      }

      public int getInitialDelay() {
         return this._initDelay;
      }

      public int getRespawnDelay() {
         return this._resDelay;
      }

      public int getDespawnDelay() {
         return this._desDelay;
      }

      public int getId() {
         return this._npcId;
      }

      public int getSpawnCount() {
         return this._spawnCount;
      }

      public List<Location> getLocationList() {
         return this._locList;
      }

      public Queue<Npc> getNpcList() {
         return this._npcList;
      }

      public List<Spawn> getSpawns() {
         List<Spawn> npcSpawns = new ArrayList<>();

         for (Npc npcInst : this._npcList) {
            npcSpawns.add(npcInst.getSpawn());
         }

         return npcSpawns;
      }

      public void setSpawnCount(int spawnCount) {
         this._spawnCount = spawnCount;
      }

      public void setRandomSpawn(boolean randValue) {
         this._randomSpawn = randValue;
      }

      public void setBroadcast(boolean broadcastValue) {
         this._broadcastAnnouncement = broadcastValue;
      }

      public boolean isSpawnActive() {
         return this._spawnActive;
      }

      public boolean isRandomSpawn() {
         return this._randomSpawn;
      }

      public boolean isBroadcasting() {
         return this._broadcastAnnouncement;
      }

      public boolean addSpawnLocation(int x, int y, int z, int heading) {
         return this._locList.add(new Location(x, y, z, heading));
      }

      public boolean addSpawnLocation(int[] spawnLoc) {
         return spawnLoc.length == 3 && this.addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
      }

      public Location removeSpawnLocation(int locIndex) {
         try {
            return this._locList.remove(locIndex);
         } catch (IndexOutOfBoundsException var3) {
            return null;
         }
      }
   }

   private class AutoSpawner implements Runnable {
      private final int _objectId;

      protected AutoSpawner(int objectId) {
         Objects.requireNonNull(AutoSpawnHandler.this);
         super();
         this._objectId = objectId;
      }

      @Override
      public void run() {
         try {
            AutoSpawnHandler.AutoSpawnInstance spawnInst = AutoSpawnHandler.this._registeredSpawns.get(this._objectId);
            if (!spawnInst.isSpawnActive()) {
               return;
            }

            List<Location> locationList = spawnInst.getLocationList();
            if (locationList.isEmpty()) {
               AutoSpawnHandler.LOGGER.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = " + this._objectId + ").");
               return;
            }

            int locationCount = locationList.size();
            int locationIndex = Rnd.get(locationCount);
            if (!spawnInst.isRandomSpawn()) {
               locationIndex = spawnInst._lastLocIndex + 1;
               if (locationIndex == locationCount) {
                  locationIndex = 0;
               }

               spawnInst._lastLocIndex = locationIndex;
            }

            int x = locationList.get(locationIndex).getX();
            int y = locationList.get(locationIndex).getY();
            int z = locationList.get(locationIndex).getZ();
            int heading = locationList.get(locationIndex).getHeading();
            Spawn newSpawn = new Spawn(spawnInst.getId());
            newSpawn.setXYZ(x, y, z);
            if (heading != -1) {
               newSpawn.setHeading(heading);
            }

            newSpawn.setAmount(spawnInst.getSpawnCount());
            if (spawnInst._desDelay == 0) {
               newSpawn.setRespawnDelay(spawnInst._resDelay);
            }

            SpawnTable.getInstance().addSpawn(newSpawn);
            Npc npcInst = null;
            if (spawnInst._spawnCount == 1) {
               npcInst = newSpawn.doSpawn();
               npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
               spawnInst.addNpc(npcInst);
            } else {
               for (int i = 0; i < spawnInst._spawnCount; i++) {
                  npcInst = newSpawn.doSpawn();
                  npcInst.setXYZ(npcInst.getX() + Rnd.get(50), npcInst.getY() + Rnd.get(50), npcInst.getZ());
                  spawnInst.addNpc(npcInst);
               }
            }

            newSpawn.stopRespawn();
            if (npcInst != null) {
               if (spawnInst.isRandomSpawn()) {
                  while (World.getFirstVisibleObjectInRange(npcInst, Npc.class, 5) != null) {
                     npcInst.teleToLocation(locationList.get(Rnd.get(locationList.size())));
                  }
               }

               if (spawnInst.isBroadcasting()) {
                  World.broadcastToAllOnlinePlayers(
                     "The " + npcInst.getName() + " has spawned near " + MapRegionData.getInstance().getClosestTownName(npcInst) + "!"
                  );
               }
            }

            if (spawnInst.getDespawnDelay() > 0) {
               ThreadPool.schedule(AutoSpawnHandler.this.new AutoDespawner(this._objectId), spawnInst.getDespawnDelay() - 1000);
            }
         } catch (Exception var12) {
            AutoSpawnHandler.LOGGER
               .log(
                  Level.WARNING,
                  "AutoSpawnHandler: An error occurred while initializing spawn instance (Object ID = " + this._objectId + "): " + var12.getMessage(),
                  (Throwable)var12
               );
         }
      }
   }

   private static class SingletonHolder {
      protected static final AutoSpawnHandler INSTANCE = new AutoSpawnHandler();
   }
}
