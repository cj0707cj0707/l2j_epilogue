package org.l2jmobius.gameserver.entity.item.auction;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.util.StatSet;

public class AuctionItem {
   private final int _auctionItemId;
   private final int _auctionLength;
   private final long _auctionInitBid;
   private final int _itemId;
   private final long _itemCount;
   private final StatSet _itemExtra;

   public AuctionItem(int auctionItemId, int auctionLength, long auctionInitBid, int itemId, long itemCount, StatSet itemExtra) {
      this._auctionItemId = auctionItemId;
      this._auctionLength = auctionLength;
      this._auctionInitBid = auctionInitBid;
      this._itemId = itemId;
      this._itemCount = itemCount;
      this._itemExtra = itemExtra;
   }

   public boolean checkItemExists() {
      return ItemData.getInstance().getTemplate(this._itemId) != null;
   }

   public int getAuctionItemId() {
      return this._auctionItemId;
   }

   public int getAuctionLength() {
      return this._auctionLength;
   }

   public long getAuctionInitBid() {
      return this._auctionInitBid;
   }

   public int getItemId() {
      return this._itemId;
   }

   public long getItemCount() {
      return this._itemCount;
   }

   public Item createNewItemInstance() {
      Item item = ItemManager.createItem(ItemProcessType.RESTORE, this._itemId, this._itemCount, null, null);
      item.setEnchantLevel(item.getDefaultEnchantLevel());
      int augmentationId = this._itemExtra.getInt("augmentation_id", 0);
      if (augmentationId > 0) {
         item.setAugmentation(new Augmentation(augmentationId));
      }

      return item;
   }
}
