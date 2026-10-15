package org.l2jmobius.gameserver.mechanics.fishing;

import org.l2jmobius.gameserver.util.StatSet;

public class FishingRod {
   private final int _fishingRodId;
   private final int _fishingRodItemId;
   private final int _fishingRodLevel;
   private final String _fishingRodName;
   private final double _fishingRodDamage;

   public FishingRod(StatSet set) {
      this._fishingRodId = set.getInt("fishingRodId");
      this._fishingRodItemId = set.getInt("fishingRodItemId");
      this._fishingRodLevel = set.getInt("fishingRodLevel");
      this._fishingRodName = set.getString("fishingRodName");
      this._fishingRodDamage = set.getDouble("fishingRodDamage");
   }

   public int getFishingRodId() {
      return this._fishingRodId;
   }

   public int getFishingRodItemId() {
      return this._fishingRodItemId;
   }

   public int getFishingRodLevel() {
      return this._fishingRodLevel;
   }

   public String getFishingRodItemName() {
      return this._fishingRodName;
   }

   public double getFishingRodDamage() {
      return this._fishingRodDamage;
   }
}
