package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExShowReceivedPostList;

public class RequestReceivedPostList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && GeneralConfig.ALLOW_MAIL) {
         player.sendPacket(new ExShowReceivedPostList(player.getObjectId()));
      }
   }
}
