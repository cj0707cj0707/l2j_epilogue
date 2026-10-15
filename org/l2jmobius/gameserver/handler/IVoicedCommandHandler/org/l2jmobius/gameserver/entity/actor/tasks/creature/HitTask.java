package org.l2jmobius.gameserver.entity.actor.tasks.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;

public class HitTask implements Runnable {
   private final Creature _creature;
   private final Creature _hitTarget;
   private final int _damage;
   private final boolean _crit;
   private final boolean _miss;
   private final byte _shld;
   private final boolean _soulshot;
   private final boolean _rechargeShots;

   public HitTask(Creature creature, Creature target, int damage, boolean crit, boolean miss, byte shld, boolean soulshot, boolean rechargeShots) {
      this._creature = creature;
      this._hitTarget = target;
      this._damage = damage;
      this._crit = crit;
      this._shld = shld;
      this._miss = miss;
      this._soulshot = soulshot;
      this._rechargeShots = rechargeShots;
   }

   @Override
   public void run() {
      if (this._creature != null) {
         this._creature.onHitTimer(this._hitTarget, this._damage, this._crit, this._miss, this._shld, this._soulshot, this._rechargeShots);
      }
   }
}
