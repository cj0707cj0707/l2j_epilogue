package org.l2jmobius.gameserver.entity;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Fence;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.taskmanagers.RandomAnimationTaskManager;

public class WorldRegion {
   private final Set<WorldObject> _visibleObjects = ConcurrentHashMap.newKeySet();
   private final Set<Door> _doors = ConcurrentHashMap.newKeySet();
   private final Set<Fence> _fences = ConcurrentHashMap.newKeySet();
   private WorldRegion[] _surroundingRegions;
   private final ConcurrentHashMap<WorldRegion, Boolean> _surroundingRegionCache = new ConcurrentHashMap<>();
   private final int _regionX;
   private final int _regionY;
   private final int _regionZ;
   private boolean _active = GeneralConfig.GRIDS_ALWAYS_ON;
   private ScheduledFuture<?> _neighborsTask = null;
   private final AtomicInteger _activeNeighbors = new AtomicInteger();

   public WorldRegion(int regionX, int regionY, int regionZ) {
      this._regionX = regionX;
      this._regionY = regionY;
      this._regionZ = regionZ;
   }

   private void switchAI(boolean isOn) {
      if (!this._visibleObjects.isEmpty()) {
         if (!isOn) {
            for (WorldObject wo : this._visibleObjects) {
               if (wo.isAttackable()) {
                  Attackable mob = wo.asAttackable();
                  mob.setTarget(null);
                  mob.stopMove(null);
                  mob.stopAllEffects();
                  mob.clearAggroList();
                  mob.getAttackByList().clear();
                  Spawn spawn = mob.getSpawn();
                  if (spawn != null && mob.calculateDistance2D(spawn) > NpcConfig.MAX_DRIFT_RANGE) {
                     mob.teleToLocation(spawn);
                  }

                  if (mob.hasAI()) {
                     mob.getAI().setIntentionIdle();
                     mob.getAI().stopAITask();
                  }

                  mob.abortAttack();
                  RandomAnimationTaskManager.getInstance().remove(mob);
               } else if (wo.isNpc()) {
                  RandomAnimationTaskManager.getInstance().remove(wo.asNpc());
               }
            }
         } else {
            for (WorldObject wox : this._visibleObjects) {
               if (wox.isAttackable()) {
                  wox.asAttackable().getStatus().startHpMpRegeneration();
                  RandomAnimationTaskManager.getInstance().add(wox.asNpc());
               } else if (wox.isNpc()) {
                  RandomAnimationTaskManager.getInstance().add(wox.asNpc());
               }
            }
         }
      }
   }

   public boolean isActive() {
      return this._active;
   }

   public void incrementActiveNeighbors() {
      this._activeNeighbors.incrementAndGet();
   }

   public void decrementActiveNeighbors() {
      this._activeNeighbors.decrementAndGet();
   }

   public boolean areNeighborsActive() {
      return GeneralConfig.GRIDS_ALWAYS_ON || this._activeNeighbors.get() > 0;
   }

   public boolean areNeighborsEmpty() {
      for (int i = 0; i < this._surroundingRegions.length; i++) {
         WorldRegion worldRegion = this._surroundingRegions[i];
         if (worldRegion.isActive()) {
            Collection<WorldObject> regionObjects = worldRegion.getVisibleObjects();
            if (!regionObjects.isEmpty()) {
               for (WorldObject wo : regionObjects) {
                  if (wo != null && wo.isPlayable()) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public synchronized void setActive(boolean value) {
      if (this._active != value) {
         this._active = value;
         if (value) {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               this._surroundingRegions[i].incrementActiveNeighbors();
            }
         } else {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               this._surroundingRegions[i].decrementActiveNeighbors();
            }
         }

         this.switchAI(value);
      }
   }

   private void startActivation() {
      this.setActive(true);
      synchronized (this) {
         if (this._neighborsTask != null) {
            this._neighborsTask.cancel(true);
            this._neighborsTask = null;
         }

         this._neighborsTask = ThreadPool.schedule(() -> {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               this._surroundingRegions[i].setActive(true);
            }
         }, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNON_TIME);
      }
   }

   private void startDeactivation() {
      synchronized (this) {
         if (this._neighborsTask != null) {
            this._neighborsTask.cancel(true);
            this._neighborsTask = null;
         }

         this._neighborsTask = ThreadPool.schedule(() -> {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               WorldRegion worldRegion = this._surroundingRegions[i];
               if (worldRegion.areNeighborsEmpty()) {
                  worldRegion.setActive(false);
               }
            }
         }, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNOFF_TIME);
      }
   }

   public synchronized void addVisibleObject(WorldObject object) {
      if (object != null) {
         this._visibleObjects.add(object);
         if (object.isDoor()) {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               this._surroundingRegions[i].addDoor(object.asDoor());
            }
         } else if (object.isFence()) {
            for (int i = 0; i < this._surroundingRegions.length; i++) {
               this._surroundingRegions[i].addFence((Fence)object);
            }
         }

         if (object.isPlayable() && !this._active && !GeneralConfig.GRIDS_ALWAYS_ON) {
            this.startActivation();
         }
      }
   }

   public synchronized void removeVisibleObject(WorldObject object) {
      if (object != null) {
         if (!this._visibleObjects.isEmpty()) {
            this._visibleObjects.remove(object);
            if (object.isDoor()) {
               for (int i = 0; i < this._surroundingRegions.length; i++) {
                  this._surroundingRegions[i].removeDoor(object.asDoor());
               }
            } else if (object.isFence()) {
               for (int i = 0; i < this._surroundingRegions.length; i++) {
                  this._surroundingRegions[i].removeFence((Fence)object);
               }
            }

            if (object.isPlayable() && this.areNeighborsEmpty() && !GeneralConfig.GRIDS_ALWAYS_ON) {
               this.startDeactivation();
            }
         }
      }
   }

   public Collection<WorldObject> getVisibleObjects() {
      return this._visibleObjects;
   }

   public void addDoor(Door door) {
      this._doors.add(door);
   }

   private void removeDoor(Door door) {
      this._doors.remove(door);
   }

   public Collection<Door> getDoors() {
      return this._doors;
   }

   public void addFence(Fence fence) {
      this._fences.add(fence);
   }

   private void removeFence(Fence fence) {
      this._fences.remove(fence);
   }

   public Collection<Fence> getFences() {
      return this._fences;
   }

   public void setSurroundingRegions(WorldRegion[] regions) {
      this._surroundingRegions = regions;

      for (int i = 0; i < this._surroundingRegions.length; i++) {
         if (this._surroundingRegions[i] == this) {
            WorldRegion first = this._surroundingRegions[0];
            this._surroundingRegions[0] = this;
            this._surroundingRegions[i] = first;
         }
      }
   }

   public WorldRegion[] getSurroundingRegions() {
      return this._surroundingRegions;
   }

   public boolean isSurroundingRegion(WorldRegion region) {
      return region == null
         ? false
         : this._surroundingRegionCache
            .computeIfAbsent(
               region,
               r -> this._regionX >= r.getRegionX() - 1
                  && this._regionX <= r.getRegionX() + 1
                  && this._regionY >= r.getRegionY() - 1
                  && this._regionY <= r.getRegionY() + 1
                  && this._regionZ >= r.getRegionZ() - 1
                  && this._regionZ <= r.getRegionZ() + 1
            );
   }

   public int getRegionX() {
      return this._regionX;
   }

   public int getRegionY() {
      return this._regionY;
   }

   public int getRegionZ() {
      return this._regionZ;
   }

   @Override
   public String toString() {
      return "(" + this._regionX + ", " + this._regionY + ", " + this._regionZ + ")";
   }
}
