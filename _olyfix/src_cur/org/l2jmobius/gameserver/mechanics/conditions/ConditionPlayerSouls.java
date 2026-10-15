package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerSouls extends Condition {
   private final int _souls;

   public ConditionPlayerSouls(int souls) {
      this._souls = souls;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.isPlayer() && effector.asPlayer().getChargedSouls() >= this._souls;
   }
}
