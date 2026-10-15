package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.enums.AbsorbCrystalType;

public class SoulCrystalDisplay {
   private Integer _fromLevel = 0;
   private Integer _chance = 0;
   private Integer _npcLvl = 0;
   private final AbsorbCrystalType _absorb;
   private final String _name;

   public SoulCrystalDisplay(int fromLevel, int chance, AbsorbCrystalType absorb, int npcLvl, String name) {
      this._fromLevel = fromLevel;
      this._chance = chance;
      this._absorb = absorb;
      this._npcLvl = npcLvl;
      this._name = name;
   }

   public Integer getFromLevel() {
      return this._fromLevel;
   }

   public Integer getChance() {
      return this._chance;
   }

   public AbsorbCrystalType getAbsorb() {
      return this._absorb;
   }

   public Integer getLevel() {
      return this._npcLvl;
   }

   public String getName() {
      return this._name;
   }
}
