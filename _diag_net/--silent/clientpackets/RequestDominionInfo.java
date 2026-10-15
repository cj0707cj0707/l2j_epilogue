package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExReplyDominionInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExShowOwnthingPos;

public class RequestDominionInfo extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(ExReplyDominionInfo.STATIC_PACKET);
         player.sendPacket(ExShowOwnthingPos.STATIC_PACKET);
      }
   }
}
