package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerHp extends Condition {
   private final int _hp;

   public ConditionPlayerHp(int hp) {
      this._hp = hp;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector != null && effector.getCurrentHp() * 100.0 / effector.getMaxHp() <= this._hp;
   }
}
