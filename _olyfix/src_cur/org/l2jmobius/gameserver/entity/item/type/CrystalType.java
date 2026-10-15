package org.l2jmobius.gameserver.entity.item.type;

public enum CrystalType {
   NONE(0, 0, 0, 0),
   D(1, 1458, 11, 90),
   C(2, 1459, 6, 45),
   B(3, 1460, 11, 67),
   A(4, 1461, 20, 145),
   S(5, 1462, 25, 250),
   S80(6, 1462, 25, 250),
   S84(7, 1462, 25, 250);

   private final int _level;
   private final int _crystalId;
   private final int _crystalEnchantBonusArmor;
   private final int _crystalEnchantBonusWeapon;

   private CrystalType(int level, int crystalId, int crystalEnchantBonusArmor, int crystalEnchantBonusWeapon) {
      this._level = level;
      this._crystalId = crystalId;
      this._crystalEnchantBonusArmor = crystalEnchantBonusArmor;
      this._crystalEnchantBonusWeapon = crystalEnchantBonusWeapon;
   }

   public int getLevel() {
      return this._level;
   }

   public int getCrystalId() {
      return this._crystalId;
   }

   public int getCrystalEnchantBonusArmor() {
      return this._crystalEnchantBonusArmor;
   }

   public int getCrystalEnchantBonusWeapon() {
      return this._crystalEnchantBonusWeapon;
   }

   public boolean isGreater(CrystalType crystalType) {
      return this.getLevel() > crystalType.getLevel();
   }

   public boolean isLesser(CrystalType crystalType) {
      return this.getLevel() < crystalType.getLevel();
   }
}
