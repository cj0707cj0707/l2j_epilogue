package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestResetNickname extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.getAppearance().setTitleColor(16777079);
         player.setTitle("");
         player.broadcastTitleInfo();
      }
   }
}
