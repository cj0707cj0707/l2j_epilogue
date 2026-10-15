package org.l2jmobius.gameserver.network.clientpackets;

import java.util.concurrent.TimeUnit;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestWithdrawalPledge extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.getClan() == null) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
         } else if (player.isClanLeader()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_CLAN_LEADER_CANNOT_WITHDRAW);
         } else if (player.isInCombat()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ONE_CANNOT_LEAVE_ONE_S_CLAN_DURING_COMBAT);
         } else {
            Clan clan = player.getClan();
            clan.removeClanMember(player.getObjectId(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(PlayerConfig.ALT_CLAN_JOIN_DAYS));
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN);
            sm.addString(player.getName());
            clan.broadcastToOnlineMembers(sm);
            clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(player.getName()));
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_THE_CLAN);
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.AFTER_LEAVING_OR_HAVING_BEEN_DISMISSED_FROM_A_CLAN_YOU_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_JOINING_ANOTHER_CLAN
            );
         }
      }
   }
}
