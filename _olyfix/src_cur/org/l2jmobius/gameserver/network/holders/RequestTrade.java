package org.l2jmobius.gameserver.network.holders;

public class RequestTrade {
   int _objectId;
   int _itemId;
   long _count;
   long _price;

   public RequestTrade(int objectId, long count, long price) {
      this._objectId = objectId;
      this._count = count;
      this._price = price;
   }

   public RequestTrade(int objectId, int itemId, long count, long price) {
      this._objectId = objectId;
      this._itemId = itemId;
      this._count = count;
      this._price = price;
   }

   public int getObjectId() {
      return this._objectId;
   }

   public int getItemId() {
      return this._itemId;
   }

   public void setCount(long count) {
      this._count = count;
   }

   public long getCount() {
      return this._count;
   }

   public long getPrice() {
      return this._price;
   }

   @Override
   public int hashCode() {
      return this._objectId;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof RequestTrade) ? false : this._objectId != ((RequestTrade)obj)._objectId;
      }
   }
}
