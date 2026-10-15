package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerAgathionId extends Condition {
   private final int _agathionId;

   public ConditionPlayerAgathionId(int agathionId) {
      this._agathionId = agathionId;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.isPlayer() && effector.asPlayer().getAgathionId() == this._agathionId;
   }
}
