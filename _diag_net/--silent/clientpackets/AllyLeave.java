package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;

public class AllyLeave extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.getClan() == null) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
         } else if (!player.isClanLeader()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ONLY_THE_CLAN_LEADER_MAY_APPLY_FOR_WITHDRAWAL_FROM_THE_ALLIANCE);
         } else {
            Clan clan = player.getClan();
            if (clan.getAllyId() == 0) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS);
            } else if (clan.getId() == clan.getAllyId()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ALLIANCE_LEADERS_CANNOT_WITHDRAW);
            } else {
               long currentTime = System.currentTimeMillis();
               clan.setAllyId(0);
               clan.setAllyName(null);
               clan.changeAllyCrest(0, true);
               clan.setAllyPenaltyExpiryTime(currentTime + PlayerConfig.ALT_ALLY_JOIN_DAYS_WHEN_LEAVED * 86400000, 1);
               clan.updateClanInDB();
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_THE_ALLIANCE);
            }
         }
      }
   }
}
