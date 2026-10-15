package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestExAcceptJoinMPCC extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getActiveRequester();
         if (requestor == null) {
            return;
         }

         if (this._response == 1) {
            boolean newCc = false;
            if (!requestor.getParty().isInCommandChannel()) {
               new CommandChannel(requestor);
               SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
               requestor.sendPacket(sm);
               newCc = true;
            }

            requestor.getParty().getCommandChannel().addParty(player.getParty());
            if (!newCc) {
               SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL);
               player.sendPacket(sm);
            }
         } else {
            requestor.sendMessage("The player declined to join your Command Channel.");
         }

         player.setActiveRequester(null);
         requestor.onTransactionResponse();
      }
   }
}
