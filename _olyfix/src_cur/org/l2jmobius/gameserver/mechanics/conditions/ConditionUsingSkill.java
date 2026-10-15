package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionUsingSkill extends Condition {
   private final int _skillId;

   public ConditionUsingSkill(int skillId) {
      this._skillId = skillId;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return skill == null ? false : skill.getId() == this._skillId;
   }
}
