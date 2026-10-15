package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public abstract class ConditionInventory extends Condition {
   protected final int _slot;

   public ConditionInventory(int slot) {
      this._slot = slot;
   }

   @Override
   public abstract boolean testImpl(Creature var1, Creature var2, Skill var3, ItemTemplate var4);
}
