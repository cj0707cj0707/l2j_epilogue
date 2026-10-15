package org.l2jmobius.gameserver.entity.item.enums;

public enum ShotType {
   SOULSHOTS,
   SPIRITSHOTS,
   BLESSED_SPIRITSHOTS,
   FISH_SOULSHOTS;

   private final int _mask = 1 << this.ordinal();

   public int getMask() {
      return this._mask;
   }
}
