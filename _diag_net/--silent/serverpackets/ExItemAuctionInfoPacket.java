package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuction;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionBid;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionState;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;

public class ExItemAuctionInfoPacket extends AbstractItemPacket {
   private final boolean _refresh;
   private final int _timeRemaining;
   private final ItemAuction _currentAuction;
   private final ItemAuction _nextAuction;

   public ExItemAuctionInfoPacket(boolean refresh, ItemAuction currentAuction, ItemAuction nextAuction) {
      if (currentAuction == null) {
         throw new NullPointerException();
      } else {
         if (currentAuction.getAuctionState() != ItemAuctionState.STARTED) {
            this._timeRemaining = 0;
         } else {
            this._timeRemaining = (int)(currentAuction.getFinishingTimeRemaining() / 1000L);
         }

         this._refresh = refresh;
         this._currentAuction = currentAuction;
         this._nextAuction = nextAuction;
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_ITEM_AUCTION_INFO.writeId(this, buffer);
      buffer.writeByte(!this._refresh);
      buffer.writeInt(this._currentAuction.getInstanceId());
      ItemAuctionBid highestBid = this._currentAuction.getHighestBid();
      buffer.writeLong(highestBid != null ? highestBid.getLastBid() : this._currentAuction.getAuctionInitBid());
      buffer.writeInt(this._timeRemaining);
      this.writeItemInfo(this._currentAuction.getItemInfo(), buffer);
      if (this._nextAuction != null) {
         buffer.writeLong(this._nextAuction.getAuctionInitBid());
         buffer.writeInt((int)(this._nextAuction.getStartingTime() / 1000L));
         this.writeItemInfo(this._nextAuction.getItemInfo(), buffer);
      }
   }

   private void writeItemInfo(ItemInfo info, WriteBuffer buffer) {
      buffer.writeInt(info.getItem().getId());
      buffer.writeInt(info.getItem().getId());
      buffer.writeInt(info.getLocation());
      buffer.writeLong(info.getCount());
      buffer.writeShort(info.getEnchant());
      buffer.writeShort(0);
      buffer.writeInt(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeInt(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
      buffer.writeShort(0);
   }
}
