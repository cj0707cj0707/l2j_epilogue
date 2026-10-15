package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetActiveSkillId extends Condition {
   private final int _skillId;
   private final int _skillLevel;

   public ConditionTargetActiveSkillId(int skillId) {
      this._skillId = skillId;
      this._skillLevel = -1;
   }

   public ConditionTargetActiveSkillId(int skillId, int skillLevel) {
      this._skillId = skillId;
      this._skillLevel = skillLevel;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      for (Skill sk : effected.getAllSkills()) {
         if (sk != null && sk.getId() == this._skillId && (this._skillLevel == -1 || this._skillLevel <= sk.getLevel())) {
            return true;
         }
      }

      return false;
   }
}
