package org.l2jmobius.gameserver.entity.item.type;

public enum ArmorType implements ItemType {
   NONE,
   LIGHT,
   HEAVY,
   MAGIC,
   SIGIL,
   SHIELD;

   final int _mask = 1 << this.ordinal() + WeaponType.values().length;

   @Override
   public int mask() {
      return this._mask;
   }
}
