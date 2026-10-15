package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerCanEscape extends Condition {
   private final boolean _value;

   public ConditionPlayerCanEscape(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean canTeleport = true;
      Player player = effector.asPlayer();
      if (player == null) {
         canTeleport = false;
      } else if (player.isOnEvent()) {
         canTeleport = false;
      } else if (player.isInDuel()) {
         canTeleport = false;
      } else if (player.isControlBlocked()) {
         canTeleport = false;
      } else if (player.isCombatFlagEquipped()) {
         canTeleport = false;
      } else if (player.isFlying() || player.isFlyingMounted()) {
         canTeleport = false;
      } else if (player.isInOlympiadMode()) {
         canTeleport = false;
      } else if (GrandBossManager.getInstance().getZone(player) != null && !player.isGM()) {
         canTeleport = false;
      }

      return this._value == canTeleport;
   }
}
