package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerLevelRange extends Condition {
   private final int[] _levels;

   public ConditionPlayerLevelRange(int[] levels) {
      this._levels = levels;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      int level = effector.getLevel();
      return level >= this._levels[0] && level <= this._levels[1];
   }
}
