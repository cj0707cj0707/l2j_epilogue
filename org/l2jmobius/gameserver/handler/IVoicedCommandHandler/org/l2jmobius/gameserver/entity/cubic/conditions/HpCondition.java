package org.l2jmobius.gameserver.entity.cubic.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;

public class HpCondition implements ICubicCondition {
   private final HpCondition.HpConditionType _type;
   private final int _hpPercent;

   public HpCondition(HpCondition.HpConditionType type, int hpPercent) {
      this._type = type;
      this._hpPercent = hpPercent;
   }

   @Override
   public boolean test(Cubic cubic, Creature owner, WorldObject target) {
      if (target != null && (target.isCreature() || target.isDoor())) {
         double hpPercent = ((Creature)(target.isDoor() ? target.asDoor() : target.asCreature())).getCurrentHpPercent();
         switch (this._type) {
            case GREATER:
               return hpPercent > this._hpPercent;
            case LESSER:
               return hpPercent < this._hpPercent;
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public static enum HpConditionType {
      GREATER,
      LESSER;
   }
}
