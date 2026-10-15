package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestReplySurrenderPledgeWar extends ClientPacket {
   private String _reqName;
   private int _answer;

   @Override
   protected void readImpl() {
      this._reqName = this.readString();
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
               org.l2jmobius.gameserver.network.PacketLogger.info(
                  this.getClass().getSimpleName() + ": Missing implementation for answer: " + this._answer + " and name: " + this._reqName + "!"
               );
            }

            player.onTransactionRequest(requestor);
         }
      }
   }
}
