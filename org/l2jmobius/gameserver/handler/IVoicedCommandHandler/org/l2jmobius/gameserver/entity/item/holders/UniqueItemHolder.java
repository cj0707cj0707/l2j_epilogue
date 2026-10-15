package org.l2jmobius.gameserver.entity.item.holders;

public class UniqueItemHolder extends ItemHolder {
   private final int _objectId;

   public UniqueItemHolder(int id, int objectId) {
      this(id, objectId, 1L);
   }

   public UniqueItemHolder(int id, int objectId, long count) {
      super(id, count);
      this._objectId = objectId;
   }

   public int getObjectId() {
      return this._objectId;
   }

   @Override
   public String toString() {
      return "[" + this.getClass().getSimpleName() + "] ID: " + this.getId() + ", object ID: " + this._objectId + ", count: " + this.getCount();
   }
}
