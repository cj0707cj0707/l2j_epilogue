package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.commons.util.Rnd;

public class MinionHolder {
   private final int _id;
   private final int _count;
   private final int _max;
   private final long _respawnTime;
   private final int _weightPoint;

   public MinionHolder(int id, int count, int max, long respawnTime, int weightPoint) {
      this._id = id;
      this._count = count;
      this._max = max;
      this._respawnTime = respawnTime;
      this._weightPoint = weightPoint;
   }

   public int getId() {
      return this._id;
   }

   public int getCount() {
      return this._max > this._count ? Rnd.get(this._count, this._max) : this._count;
   }

   public long getRespawnTime() {
      return this._respawnTime;
   }

   public int getWeightPoint() {
      return this._weightPoint;
   }
}
