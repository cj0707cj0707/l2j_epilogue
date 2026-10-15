package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetAbnormal extends Condition {
   private final int _abnormalId;

   public ConditionTargetAbnormal(int abnormalId) {
      this._abnormalId = abnormalId;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return (effected.getAbnormalVisualEffects() & this._abnormalId) != 0;
   }
}
