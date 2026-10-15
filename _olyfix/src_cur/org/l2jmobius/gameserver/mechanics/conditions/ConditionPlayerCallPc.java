package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ConditionPlayerCallPc extends Condition {
   private final boolean _value;

   public ConditionPlayerCallPc(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean canCallPlayer = true;
      Player player = effector.asPlayer();
      if (player == null) {
         canCallPlayer = false;
      } else if (effected != null && effected.isPlayer() && effected.isDead()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
         sm.addPcName(effected.asPlayer());
         player.sendPacket(sm);
         canCallPlayer = false;
      } else if (player.isInOlympiadMode()) {
         player.sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
         canCallPlayer = false;
      } else if (player.inObserverMode()) {
         canCallPlayer = false;
      } else if (player.isOnEvent()) {
         player.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
         canCallPlayer = false;
      } else if (player.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || player.isInsideZone(ZoneId.JAIL) || player.isFlyingMounted()) {
         player.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
         canCallPlayer = false;
      }

      return this._value == canCallPlayer;
   }
}
