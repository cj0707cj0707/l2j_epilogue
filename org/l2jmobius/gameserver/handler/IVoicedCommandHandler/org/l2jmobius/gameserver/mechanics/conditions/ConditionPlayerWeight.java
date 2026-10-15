package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerWeight extends Condition {
   private final int _weight;

   public ConditionPlayerWeight(int weight) {
      this._weight = weight;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player != null && player.getMaxLoad() > 0) {
         int weightproc = (player.getCurrentLoad() - player.getBonusWeightPenalty()) * 100 / player.getMaxLoad();
         return weightproc < this._weight || player.getDietMode();
      } else {
         return true;
      }
   }
}
