package org.l2jmobius.gameserver.entity.item.holders;

public class PremiumItem {
   private final int _itemId;
   private long _count;
   private final String _sender;

   public PremiumItem(int itemid, long count, String sender) {
      this._itemId = itemid;
      this._count = count;
      this._sender = sender;
   }

   public void updateCount(long newcount) {
      this._count = newcount;
   }

   public int getItemId() {
      return this._itemId;
   }

   public long getCount() {
      return this._count;
   }

   public String getSender() {
      return this._sender;
   }
}
