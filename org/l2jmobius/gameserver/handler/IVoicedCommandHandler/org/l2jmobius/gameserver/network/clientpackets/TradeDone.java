package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.TradeList;

public class TradeDone extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are trading too fast.");
         } else {
            TradeList trade = player.getActiveTradeList();
            if (trade != null) {
               if (!trade.isLocked()) {
                  if (this._response == 1) {
                     if (trade.getPartner() == null || World.getPlayer(trade.getPartner().getObjectId()) == null) {
                        player.cancelActiveTrade();
                        player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
                        return;
                     }

                     if (trade.getOwner().getActiveEnchantItemId() != -1 || trade.getPartner().getActiveEnchantItemId() != -1) {
                        return;
                     }

                     if (!player.getAccessLevel().allowTransaction()) {
                        player.cancelActiveTrade();
                        player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
                        return;
                     }

                     if (player.getInstanceId() != trade.getPartner().getInstanceId() && player.getInstanceId() != -1) {
                        player.cancelActiveTrade();
                        return;
                     }

                     if (player.calculateDistance3D(trade.getPartner()) > 150.0) {
                        player.cancelActiveTrade();
                        return;
                     }

                     trade.confirm();
                  } else {
                     player.cancelActiveTrade();
                  }
               }
            }
         }
      }
   }
}
