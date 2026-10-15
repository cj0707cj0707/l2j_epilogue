package org.l2jmobius.gameserver.entity.spawns;

import java.util.ArrayList;
import java.util.List;

public class SpawnGroupEntry {
   private final int _total;
   private final int _respawnDelay;
   private final int _respawnRandom;
   private final List<Spawn> _slots = new ArrayList<>();
   private int _aliveOrReserved = 0;

   public SpawnGroupEntry(int total, int respawnDelay, int respawnRandom) {
      this._total = total;
      this._respawnDelay = respawnDelay;
      this._respawnRandom = respawnRandom;
   }

   public int getTotal() {
      return this._total;
   }

   public int getRespawnDelay() {
      return this._respawnDelay;
   }

   public int getRespawnRandom() {
      return this._respawnRandom;
   }

   public List<Spawn> getSlots() {
      return this._slots;
   }

   public void addSlot(Spawn spawn) {
      this._slots.add(spawn);
   }

   public int getAliveOrReserved() {
      return this._aliveOrReserved;
   }

   public void setAliveOrReserved(int count) {
      this._aliveOrReserved = count;
   }
}
