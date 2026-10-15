package org.l2jmobius.gameserver.entity.cubic.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;

public class HealthCondition implements ICubicCondition {
   private final int _min;
   private final int _max;

   public HealthCondition(int min, int max) {
      this._min = min;
      this._max = max;
   }

   @Override
   public boolean test(Cubic cubic, Creature owner, WorldObject target) {
      if (target != null && (target.isCreature() || target.isDoor())) {
         double hpPercent = ((Creature)(target.isDoor() ? target.asDoor() : target.asCreature())).getCurrentHpPercent();
         return hpPercent > this._min && hpPercent < this._max;
      } else {
         return false;
      }
   }
}
