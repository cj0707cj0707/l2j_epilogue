package org.l2jmobius.gameserver.entity.item.enums;

public enum ElementalItemType {
   STONE(3),
   ROUGH_ORE(3),
   CRYSTAL(6),
   JEWEL(9),
   ENERGY(12);

   private final int _maxLevel;

   private ElementalItemType(int maxLevel) {
      this._maxLevel = maxLevel;
   }

   public int getMaxLevel() {
      return this._maxLevel;
   }
}
