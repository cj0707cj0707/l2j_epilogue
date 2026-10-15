package org.l2jmobius.gameserver.entity.cubic.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;

public class RangeCondition implements ICubicCondition {
   private final int _range;

   public RangeCondition(int range) {
      this._range = range;
   }

   @Override
   public boolean test(Cubic cubic, Creature owner, WorldObject target) {
      return owner != null && target != null && owner.calculateDistance2D(target) <= this._range;
   }
}
