package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.enums.AbsorbCrystalType;

public final class LevelingSoulCrystalInfo {
   private final AbsorbCrystalType _absorbCrystalType;
   private final boolean _isSkillNeeded;
   private final int _chance;

   public LevelingSoulCrystalInfo(AbsorbCrystalType absorbCrystalType, boolean isSkillNeeded, int chance) {
      this._absorbCrystalType = absorbCrystalType;
      this._isSkillNeeded = isSkillNeeded;
      this._chance = chance;
   }

   public AbsorbCrystalType getAbsorbCrystalType() {
      return this._absorbCrystalType;
   }

   public int getChance() {
      return this._chance;
   }

   public boolean isSkillNeeded() {
      return this._isSkillNeeded;
   }
}
