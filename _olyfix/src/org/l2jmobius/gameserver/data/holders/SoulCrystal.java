package org.l2jmobius.gameserver.data.holders;

public final class SoulCrystal {
   private final int _level;
   private final int _itemId;
   private final int _leveledItemId;

   public SoulCrystal(int level, int itemId, int leveledItemId) {
      this._level = level;
      this._itemId = itemId;
      this._leveledItemId = leveledItemId;
   }

   public int getItemId() {
      return this._itemId;
   }

   public int getLevel() {
      return this._level;
   }

   public int getLeveledItemId() {
      return this._leveledItemId;
   }
}
