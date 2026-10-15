package org.l2jmobius.gameserver.data.holders;

public class RangeChanceHolder {
   private final int _min;
   private final int _max;
   private final double _chance;

   public RangeChanceHolder(int min, int max, double chance) {
      this._min = min;
      this._max = max;
      this._chance = chance;
   }

   public int getMin() {
      return this._min;
   }

   public int getMax() {
      return this._max;
   }

   public double getChance() {
      return this._chance;
   }
}
