package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetLevelRange extends Condition {
   private final int[] _levels;

   public ConditionTargetLevelRange(int[] levels) {
      this._levels = levels;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effected == null) {
         return false;
      } else {
         int level = effected.getLevel();
         return level >= this._levels[0] && level <= this._levels[1];
      }
   }
}
