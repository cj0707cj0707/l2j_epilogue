package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;

public class RequestPledgeMemberList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            player.sendPacket(new PledgeShowMemberListAll(clan, player));
         }
      }
   }
}
