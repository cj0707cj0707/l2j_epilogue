package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionUsingItemType extends Condition {
   private final boolean _armor;
   private final int _mask;

   public ConditionUsingItemType(int mask) {
      this._mask = mask;
      this._armor = (this._mask & (ArmorType.MAGIC.mask() | ArmorType.LIGHT.mask() | ArmorType.HEAVY.mask())) != 0;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector == null) {
         return false;
      } else if (effector.isPlayer()) {
         Inventory inv = effector.getInventory();
         if (this._armor) {
            Item chest = inv.getPaperdollItem(6);
            Item legs = inv.getPaperdollItem(11);
            if (chest == null && legs == null) {
               return (ArmorType.NONE.mask() & this._mask) == ArmorType.NONE.mask();
            }

            if (chest != null) {
               if (legs == null) {
                  BodyPart chestBodyPart = chest.getTemplate().getBodyPart();
                  if (chestBodyPart == BodyPart.FULL_ARMOR) {
                     int chestMask = chest.getTemplate().getItemMask();
                     return (this._mask & chestMask) != 0;
                  }

                  return (ArmorType.NONE.mask() & this._mask) == ArmorType.NONE.mask();
               }

               int chestMask = chest.getTemplate().getItemMask();
               int legMask = legs.getTemplate().getItemMask();
               if (chestMask == legMask) {
                  return (this._mask & chestMask) != 0;
               }

               return (ArmorType.NONE.mask() & this._mask) == ArmorType.NONE.mask();
            }
         }

         return (this._mask & inv.getWearedMask()) != 0;
      } else {
         return !this._armor && (this._mask & effector.getAttackType().mask()) != 0;
      }
   }
}
