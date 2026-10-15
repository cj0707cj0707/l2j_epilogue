package org.l2jmobius.gameserver.entity.spawns;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager;

public class SpawnGroup {
   private static final Logger LOGGER = Logger.getLogger(SpawnGroup.class.getName());
   private final String _name;
   private final SpawnSelection _selection;
   private final int _maximumNpc;
   private final List<SpawnGroupEntry> _entries = new ArrayList<>();
   private int _aliveOrReserved = 0;
   private boolean _active = true;

   public SpawnGroup(String name, SpawnSelection selection, int maximumNpc) {
      this._name = name;
      this._selection = selection;
      this._maximumNpc = maximumNpc;
   }

   public String getName() {
      return this._name;
   }

   public SpawnSelection getSelection() {
      return this._selection;
   }

   public int getMaximumNpc() {
      return this._maximumNpc;
   }

   public List<SpawnGroupEntry> getEntries() {
      return this._entries;
   }

   public void addEntry(SpawnGroupEntry entry) {
      this._entries.add(entry);
   }

   public synchronized int getAliveOrReserved() {
      return this._aliveOrReserved;
   }

   public synchronized boolean tryReserve(SpawnGroupEntry entry, int count) {
      if (this._selection != SpawnSelection.RANDOM && this._maximumNpc > 0 && this._aliveOrReserved + count > this._maximumNpc) {
         return false;
      } else if (entry.getAliveOrReserved() + count > entry.getTotal()) {
         return false;
      } else {
         entry.setAliveOrReserved(entry.getAliveOrReserved() + count);
         this._aliveOrReserved += count;
         return true;
      }
   }

   public synchronized void release(SpawnGroupEntry entry, int count) {
      entry.setAliveOrReserved(entry.getAliveOrReserved() - count);
      this._aliveOrReserved -= count;
   }

   public int initialSpawn() {
      if (this._entries.isEmpty()) {
         LOGGER.warning(this.getClass().getSimpleName() + ": " + this._name + " has no valid entries to spawn.");
         return 0;
      } else {
         int spawned = 0;
         switch (this._selection) {
            case ALL:
               for (SpawnGroupEntry entryx : this._entries) {
                  if (this.tryReserve(entryx, entryx.getTotal())) {
                     spawned += this.spawnUnits(entryx, entryx.getTotal());
                  }
               }
               break;
            case RANDOM:
               SpawnGroupEntry entry = this._entries.get(Rnd.get(this._entries.size()));
               if (this.tryReserve(entry, entry.getTotal())) {
                  spawned += this.spawnUnits(entry, entry.getTotal());
               }
               break;
            case RANDOM_FILL:
               for (int i = 0; i < this._maximumNpc; i++) {
                  SpawnGroupEntry entryx = this._entries.get(Rnd.get(this._entries.size()));
                  if (this.tryReserve(entryx, 1)) {
                     spawned += this.spawnUnits(entryx, 1);
                  }
               }
         }

         return spawned;
      }
   }

   public void onUnitDeath(Spawn spawn) {
      if (this._selection != SpawnSelection.ALL) {
         SpawnGroupEntry deadEntry = this.getEntryBySlot(spawn);
         if (deadEntry != null) {
            synchronized (this) {
               deadEntry.setAliveOrReserved(deadEntry.getAliveOrReserved() - 1);
               this._aliveOrReserved--;
            }

            if (this._active) {
               SpawnGroupEntry rolled = this._entries.get(Rnd.get(this._entries.size()));
               if (this.tryReserve(rolled, 1)) {
                  int respawnDelay = rolled.getRespawnDelay();
                  int respawnRandom = rolled.getRespawnRandom();
                  int minDelay = Math.max(1, respawnDelay - respawnRandom) * 1000;
                  int maxDelay = Math.max(1, respawnDelay + respawnRandom) * 1000;
                  SpawnGroupTaskManager.getInstance()
                     .add(this, rolled, System.currentTimeMillis() + (respawnRandom > 0 ? Rnd.get(minDelay, maxDelay) : minDelay));
               }
            }
         }
      }
   }

   public boolean isActive() {
      return this._active;
   }

   public void setActive(boolean active) {
      this._active = active;
   }

   public SpawnGroupEntry getEntryBySlot(Spawn spawn) {
      for (SpawnGroupEntry entry : this._entries) {
         for (Spawn slot : entry.getSlots()) {
            if (slot == spawn) {
               return entry;
            }
         }
      }

      return null;
   }

   public int spawnUnits(SpawnGroupEntry entry, int count) {
      if (!this._active) {
         this.release(entry, count);
         return 0;
      } else {
         int spawned = 0;
         List<Spawn> slots = entry.getSlots();

         for (int i = 0; i < count; i++) {
            Spawn slot = slots.get(Rnd.get(slots.size()));
            if (slot.doSpawn(false) == null) {
               this.release(entry, 1);
            } else {
               spawned++;
            }
         }

         return spawned;
      }
   }
}
