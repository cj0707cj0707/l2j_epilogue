package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanTransform extends Condition {
   private final boolean _value;

   public ConditionPlayerCanTransform(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean canTransform = true;
      Player player = effector.asPlayer();
      if (player == null || player.isAlikeDead() || player.isCursedWeaponEquipped()) {
         canTransform = false;
      } else if (player.isSitting()) {
         player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFORM_WHILE_SITTING);
         canTransform = false;
      } else if (player.isTransformed() || player.isInStance()) {
         player.sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
         canTransform = false;
      } else if (player.isInWater()) {
         player.sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_INTO_THE_DESIRED_FORM_IN_WATER);
         canTransform = false;
      } else if (player.isFlyingMounted() || player.isMounted()) {
         player.sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_WHILE_RIDING_A_PET);
         canTransform = false;
      } else if (player.isRegisteredOnEvent()) {
         player.sendMessage("You cannot transform while registered on an event.");
         canTransform = false;
      }

      return this._value == canTransform;
   }
}
