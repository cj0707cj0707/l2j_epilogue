package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetLevel extends Condition {
   private final int _level;

   public ConditionTargetLevel(int level) {
      this._level = level;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effected == null ? false : effected.getLevel() >= this._level;
   }
}
