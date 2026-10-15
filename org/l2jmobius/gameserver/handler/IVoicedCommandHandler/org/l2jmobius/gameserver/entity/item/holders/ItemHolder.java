package org.l2jmobius.gameserver.entity.item.holders;

public class ItemHolder {
   private final int _id;
   private final long _count;

   public ItemHolder(int id, long count) {
      this._id = id;
      this._count = count;
   }

   public int getId() {
      return this._id;
   }

   public long getCount() {
      return this._count;
   }

   @Override
   public String toString() {
      return "[" + this.getClass().getSimpleName() + "] ID: " + this._id + ", count: " + this._count;
   }
}
