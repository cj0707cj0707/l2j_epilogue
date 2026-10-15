package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionSlotItemType extends ConditionInventory {
   private final int _mask;

   public ConditionSlotItemType(int slot, int mask) {
      super(slot);
      this._mask = mask;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Item itemSlot = effector.getInventory().getPaperdollItem(this._slot);
         return itemSlot == null ? false : (itemSlot.getTemplate().getItemMask() & this._mask) != 0;
      } else {
         return false;
      }
   }
}
