package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class ConditionGameTime extends Condition {
   private final boolean _required;

   public ConditionGameTime(boolean required) {
      this._required = required;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return GameTimeTaskManager.getInstance().isNight() == this._required;
   }
}
