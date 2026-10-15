package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExShowCastleInfo;
import org.l2jmobius.gameserver.network.serverpackets.PartyMemberPosition;

public class RequestAllCastleInfo extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new ExShowCastleInfo());
         if (player.isInParty()) {
            player.sendPacket(new PartyMemberPosition(player.getParty()));
         }
      }
   }
}
