package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.WorldObject;

public class Hit {
   private static final int HITFLAG_USESS = 16;
   private static final int HITFLAG_CRIT = 32;
   private static final int HITFLAG_SHLD = 64;
   private static final int HITFLAG_MISS = 128;
   private final int _targetId;
   private final int _damage;
   private int _flags = 0;

   public Hit(WorldObject target, int damage, boolean miss, boolean crit, byte shld, boolean soulshot, int ssGrade) {
      this._targetId = target.getObjectId();
      this._damage = damage;
      if (soulshot) {
         this._flags |= 16 | ssGrade;
      }

      if (crit) {
         this._flags |= 32;
      }

      if (shld > 0) {
         this._flags |= 64;
      }

      if (miss) {
         this._flags |= 128;
      }
   }

   public int getTargetId() {
      return this._targetId;
   }

   public int getDamage() {
      return this._damage;
   }

   public int getFlags() {
      return this._flags;
   }
}
