package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerCheckAbnormal extends Condition {
   private final AbnormalType _type;
   private final int _level;

   public ConditionPlayerCheckAbnormal(AbnormalType type) {
      this._type = type;
      this._level = -1;
   }

   public ConditionPlayerCheckAbnormal(AbnormalType type, int level) {
      this._type = type;
      this._level = level;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      BuffInfo info = effector.getEffectList().getBuffInfoByAbnormalType(this._type);
      return info != null && (this._level == -1 || this._level >= info.getSkill().getAbnormalLevel());
   }
}
