package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionUsingSlotType extends Condition {
   private final int _mask;

   public ConditionUsingSlotType(int mask) {
      this._mask = mask;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Weapon activeWeapon = effector.getActiveWeaponItem();
         return activeWeapon == null ? false : (activeWeapon.getBodyPart().getMask() & this._mask) != 0;
      } else {
         return false;
      }
   }
}
