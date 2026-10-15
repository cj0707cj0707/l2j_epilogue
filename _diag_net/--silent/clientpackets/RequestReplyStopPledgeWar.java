package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestReplyStopPledgeWar extends ClientPacket {
   private int _answer;

   @Override
   protected void readImpl() {
      this.readString();
      this._answer = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getActiveRequester();
         if (requestor != null) {
            if (this._answer == 1) {
               ClanTable.getInstance().deleteClanWars(requestor.getClanId(), player.getClanId());
            } else {
               requestor.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.REQUEST_TO_END_WAR_HAS_BEEN_DENIED);
            }

            player.setActiveRequester(null);
            requestor.onTransactionResponse();
         }
      }
   }
}
