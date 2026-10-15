package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;

public class AggroInfo {
   private static final long LIMIT = 1000000000000000L;
   private final Creature _aggressor;
   private long _damageTotal;
   private long _hateTotal;

   public AggroInfo(Creature aggressor) {
      this._aggressor = aggressor;
      this._damageTotal = 0L;
      this._hateTotal = 0L;
   }

   public Creature getAttacker() {
      return this._aggressor;
   }

   public long getDamage() {
      return this._damageTotal;
   }

   public void addDamage(long value) {
      if (value != 0L) {
         if (value > 0L) {
            long current = this._damageTotal;
            this._damageTotal = current >= 1000000000000000L - value ? 1000000000000000L : current + value;
         } else {
            this._damageTotal += value;
         }
      }
   }

   public long getHate() {
      return this._hateTotal;
   }

   public void addHate(long value) {
      if (value != 0L) {
         if (value > 0L) {
            long current = this._hateTotal;
            this._hateTotal = current >= 1000000000000000L - value ? 1000000000000000L : current + value;
         } else {
            this._hateTotal += value;
         }
      }
   }

   public void stopHate() {
      this._hateTotal = 0L;
   }

   public long checkHate(Creature owner) {
      Creature attacker = this._aggressor;
      if (attacker == null || owner == null) {
         this._hateTotal = 0L;
         return 0L;
      } else if (attacker.isAlikeDead()) {
         this._hateTotal = 0L;
         return 0L;
      } else if (!attacker.isSpawned()) {
         this._hateTotal = 0L;
         return 0L;
      } else {
         if (!owner.isInSurroundingRegion(attacker)) {
            this._hateTotal = 0L;
         }

         return this._hateTotal;
      }
   }

   @Override
   public int hashCode() {
      Creature c = this._aggressor;
      return c != null ? c.getObjectId() : 0;
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else {
         return !(other instanceof AggroInfo) ? false : ((AggroInfo)other)._aggressor == this._aggressor;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(96);
      sb.append("AggroInfo [attacker=");
      sb.append(this._aggressor);
      sb.append(", hate=");
      sb.append(this._hateTotal);
      sb.append(", damage=");
      sb.append(this._damageTotal);
      sb.append(']');
      return sb.toString();
   }
}
