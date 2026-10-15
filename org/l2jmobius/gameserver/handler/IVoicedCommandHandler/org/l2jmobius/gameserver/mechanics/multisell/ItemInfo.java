package org.l2jmobius.gameserver.mechanics.multisell;

import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ItemInfo {
   private final int _enchantLevel;
   private final int _augmentId;
   private final byte _elementId;
   private final int _elementPower;
   private final int[] _elementals = new int[6];

   public ItemInfo(Item item) {
      this._enchantLevel = item.getEnchantLevel();
      this._augmentId = item.getAugmentation() != null ? item.getAugmentation().getAugmentationId() : 0;
      this._elementId = item.getAttackElementType();
      this._elementPower = item.getAttackElementPower();
      this._elementals[0] = item.getElementDefAttr((byte)0);
      this._elementals[1] = item.getElementDefAttr((byte)1);
      this._elementals[2] = item.getElementDefAttr((byte)2);
      this._elementals[3] = item.getElementDefAttr((byte)3);
      this._elementals[4] = item.getElementDefAttr((byte)4);
      this._elementals[5] = item.getElementDefAttr((byte)5);
   }

   public ItemInfo(int enchantLevel) {
      this._enchantLevel = enchantLevel;
      this._augmentId = 0;
      this._elementId = -1;
      this._elementPower = 0;
      this._elementals[0] = 0;
      this._elementals[1] = 0;
      this._elementals[2] = 0;
      this._elementals[3] = 0;
      this._elementals[4] = 0;
      this._elementals[5] = 0;
   }

   public int getEnchantLevel() {
      return this._enchantLevel;
   }

   public int getAugmentId() {
      return this._augmentId;
   }

   public byte getElementId() {
      return this._elementId;
   }

   public int getElementPower() {
      return this._elementPower;
   }

   public int[] getElementals() {
      return this._elementals;
   }
}
