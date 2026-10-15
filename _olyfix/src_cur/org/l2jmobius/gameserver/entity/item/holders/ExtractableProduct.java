package org.l2jmobius.gameserver.entity.item.holders;

public class ExtractableProduct {
   private final int _id;
   private final long _min;
   private final long _max;
   private final int _chance;
   private final int _minEnchant;
   private final int _maxEnchant;

   public ExtractableProduct(int id, long min, long max, double chance, int minEnchant, int maxEnchant) {
      this._id = id;
      this._min = min;
      this._max = max;
      this._chance = (int)(chance * 1000.0);
      this._minEnchant = minEnchant;
      this._maxEnchant = maxEnchant;
   }

   public int getId() {
      return this._id;
   }

   public long getMin() {
      return this._min;
   }

   public long getMax() {
      return this._max;
   }

   public int getChance() {
      return this._chance;
   }

   public int getMinEnchant() {
      return this._minEnchant;
   }

   public int getMaxEnchant() {
      return this._maxEnchant;
   }
}
