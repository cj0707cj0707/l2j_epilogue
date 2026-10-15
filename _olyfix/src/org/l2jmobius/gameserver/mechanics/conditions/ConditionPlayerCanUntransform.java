package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanUntransform extends Condition {
   private final boolean _value;

   public ConditionPlayerCanUntransform(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean canUntransform = true;
      Player player = effector.asPlayer();
      if (player == null) {
         canUntransform = false;
      } else if (player.isAlikeDead() || player.isCursedWeaponEquipped()) {
         canUntransform = false;
      } else if ((player.isTransformed() || player.isInStance()) && player.isFlyingMounted() && !player.isInsideZone(ZoneId.LANDING)) {
         player.sendPacket(SystemMessageId.BOARDING_OR_CANCELLATION_OF_BOARDING_ON_AIRSHIPS_IS_NOT_ALLOWED_IN_THE_CURRENT_AREA);
         canUntransform = false;
      }

      return this._value == canUntransform;
   }
}
