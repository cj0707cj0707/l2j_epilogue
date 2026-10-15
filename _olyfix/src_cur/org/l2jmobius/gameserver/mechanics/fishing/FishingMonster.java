package org.l2jmobius.gameserver.mechanics.fishing;

import org.l2jmobius.gameserver.util.StatSet;

public class FishingMonster {
   private final int _userMinLevel;
   private final int _userMaxLevel;
   private final int _fishingMonsterId;
   private final int _probability;

   public FishingMonster(StatSet set) {
      this._userMinLevel = set.getInt("userMinLevel");
      this._userMaxLevel = set.getInt("userMaxLevel");
      this._fishingMonsterId = set.getInt("fishingMonsterId");
      this._probability = set.getInt("probability");
   }

   public int getUserMinLevel() {
      return this._userMinLevel;
   }

   public int getUserMaxLevel() {
      return this._userMaxLevel;
   }

   public int getFishingMonsterId() {
      return this._fishingMonsterId;
   }

   public int getProbability() {
      return this._probability;
   }
}
