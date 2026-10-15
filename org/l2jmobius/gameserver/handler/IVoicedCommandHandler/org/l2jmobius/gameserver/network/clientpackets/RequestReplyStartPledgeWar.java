package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class RequestReplyStartPledgeWar extends ClientPacket {
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
               ClanTable.getInstance().storeClanWars(requestor.getClanId(), player.getClanId());
            } else {
               requestor.sendPacket(SystemMessageId.THE_S1_CLAN_DID_NOT_RESPOND_WAR_PROCLAMATION_HAS_BEEN_REFUSED_2);
            }

            player.setActiveRequester(null);
            requestor.onTransactionResponse();
         }
      }
   }
}
