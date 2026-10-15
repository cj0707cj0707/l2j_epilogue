package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetWeight extends Condition {
   private final int _weight;

   public ConditionTargetWeight(int weight) {
      this._weight = weight;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effected != null && effected.isPlayer()) {
         Player target = effected.asPlayer();
         if (!target.getDietMode() && target.getMaxLoad() > 0) {
            return (target.getCurrentLoad() - target.getBonusWeightPenalty()) * 100 / target.getMaxLoad() < this._weight;
         }
      }

      return false;
   }
}
