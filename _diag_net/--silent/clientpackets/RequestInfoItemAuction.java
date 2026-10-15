package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuction;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionInstance;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAuctionInfoPacket;

public class RequestInfoItemAuction extends ClientPacket {
   private int _instanceId;

   @Override
   protected void readImpl() {
      this._instanceId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canUseItemAuction()) {
            ItemAuctionInstance instance = ItemAuctionManager.getInstance().getManagerInstance(this._instanceId);
            if (instance != null) {
               ItemAuction auction = instance.getCurrentAuction();
               if (auction != null) {
                  player.updateLastItemAuctionRequest();
                  player.sendPacket(new ExItemAuctionInfoPacket(true, auction, instance.getNextAuction()));
               }
            }
         }
      }
   }
}
