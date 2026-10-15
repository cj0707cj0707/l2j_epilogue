package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class DistrustAI extends AttackableAI {
   private final Creature _forcedTarget;

   public DistrustAI(Attackable actor, Creature forcedTarget) {
      super(actor);
      this._forcedTarget = forcedTarget;
   }

   @Override
   public void thinkAttack() {
      if (this._forcedTarget != null && !this._forcedTarget.isDead()) {
         this._actor.setTarget(this._forcedTarget);
         this.setIntentionAttack(this._forcedTarget);
         int range = this._actor.getPhysicalAttackRange() + this._forcedTarget.getTemplate().getCollisionRadius();
         if (this._actor.calculateDistance3D(this._forcedTarget) > range) {
            this.moveToPawn(this._forcedTarget, range);
         } else if (GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)this._forcedTarget)) {
            this._actor.doAttack(this._forcedTarget);
         }
      } else {
         this._actor.setTarget(null);
         this.setIntentionActive();
      }
   }
}
