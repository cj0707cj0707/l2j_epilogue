package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerMp extends Condition {
   private final int _mp;

   public ConditionPlayerMp(int mp) {
      this._mp = mp;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.getCurrentMp() * 100.0 / effector.getMaxMp() <= this._mp;
   }
}
