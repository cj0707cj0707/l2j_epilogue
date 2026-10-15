package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuction;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionInstance;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;

public class RequestBidItemAuction extends ClientPacket {
   private int _instanceId;
   private long _bid;

   @Override
   protected void readImpl() {
      this._instanceId = this.readInt();
      this._bid = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are bidding too fast.");
         } else if (this._bid >= 0L && this._bid <= Inventory.MAX_ADENA) {
            ItemAuctionInstance instance = ItemAuctionManager.getInstance().getManagerInstance(this._instanceId);
            if (instance != null) {
               ItemAuction auction = instance.getCurrentAuction();
               if (auction != null) {
                  auction.registerBid(player, this._bid);
               }
            }
         }
      }
   }
}
