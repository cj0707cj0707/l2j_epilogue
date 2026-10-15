package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionChangeWeapon extends Condition {
   private final boolean _required;

   public ConditionChangeWeapon(boolean required) {
      this._required = required;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player == null) {
         return false;
      } else {
         if (this._required) {
            Weapon weaponItem = effector.getActiveWeaponItem();
            if (weaponItem == null) {
               return false;
            }

            if (weaponItem.getChangeWeaponId() == 0) {
               return false;
            }

            if (player.isEnchanting()) {
               return false;
            }
         }

         return true;
      }
   }
}
