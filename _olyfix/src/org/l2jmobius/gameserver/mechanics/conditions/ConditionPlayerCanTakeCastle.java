package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public class ConditionPlayerCanTakeCastle extends Condition {
   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Player player = effector.asPlayer();
         if (!player.isAlikeDead() && !player.isCursedWeaponEquipped() && player.isClanLeader()) {
            Castle castle = CastleManager.getInstance().getCastle(player);
            if (castle == null
               || castle.getResidenceId() <= 0
               || !castle.getSiege().isInProgress()
               || castle.getSiege().getAttackerClan(player.getClan()) == null) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
               sm.addSkillName(skill);
               player.sendPacket(sm);
               return false;
            } else if (!castle.getArtefacts().contains(effected)) {
               player.sendPacket(SystemMessageId.INVALID_TARGET);
               return false;
            } else if (LocationUtil.checkIfInRange(200, player, effected, true)
               && player.getZ() >= effected.getZ()
               && Math.abs(player.getZ() - effected.getZ()) <= 45) {
               castle.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_OPPOSING_CLAN_HAS_STARTED_TO_ENGRAVE_THE_MONUMENT), false);
               return true;
            } else {
               player.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
