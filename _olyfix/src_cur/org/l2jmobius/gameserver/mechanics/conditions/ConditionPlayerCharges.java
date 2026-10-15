package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerCharges extends Condition {
   private final int _charges;

   public ConditionPlayerCharges(int charges) {
      this._charges = charges;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.isPlayer() && effector.asPlayer().getCharges() >= this._charges;
   }
}
