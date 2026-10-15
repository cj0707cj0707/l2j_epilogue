package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class AnswerTradeRequest extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            Player partner = player.getActiveRequester();
            if (partner == null) {
               player.sendPacket(new org.l2jmobius.gameserver.network.serverpackets.TradeDone(0));
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
               player.setActiveRequester(null);
            } else if (World.getPlayer(partner.getObjectId()) == null) {
               player.sendPacket(new org.l2jmobius.gameserver.network.serverpackets.TradeDone(0));
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
               player.setActiveRequester(null);
            } else {
               if (this._response == 1 && !partner.isRequestExpired()) {
                  player.startTrade(partner);
               } else {
                  SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_HAS_DENIED_YOUR_REQUEST_TO_TRADE);
                  msg.addString(player.getName());
                  partner.sendPacket(msg);
               }

               player.setActiveRequester(null);
               partner.onTransactionResponse();
            }
         }
      }
   }
}
