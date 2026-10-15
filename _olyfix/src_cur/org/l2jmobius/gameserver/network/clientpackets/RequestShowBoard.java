package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;

public class RequestShowBoard extends ClientPacket {
   @Override
   protected void readImpl() {
      this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         CommunityBoardHandler.getInstance().handleParseCommand(GeneralConfig.BBS_DEFAULT, player);
      }
   }
}
