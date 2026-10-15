package org.l2jmobius.gameserver.entity.item;

import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;

public class WarehouseItem {
   private final ItemTemplate _item;
   private final int _object;
   private final long _count;
   private final int _owner;
   private final int _locationSlot;
   private final int _enchant;
   private final CrystalType _grade;
   private boolean _isAugmented;
   private int _augmentationId;
   private final int _customType1;
   private final int _customType2;
   private final int _mana;
   private int _elemAtkType = -2;
   private int _elemAtkPower = 0;
   private final int[] _elemDefAttr = new int[]{0, 0, 0, 0, 0, 0};
   private final int[] _enchantOptions;
   private final int _time;

   public WarehouseItem(Item item) {
      this._item = item.getTemplate();
      this._object = item.getObjectId();
      this._count = item.getCount();
      this._owner = item.getOwnerId();
      this._locationSlot = item.getLocationSlot();
      this._enchant = item.getEnchantLevel();
      this._customType1 = item.getCustomType1();
      this._customType2 = item.getCustomType2();
      this._grade = item.getTemplate().getCrystalType();
      if (item.isAugmented()) {
         this._isAugmented = true;
         this._augmentationId = item.getAugmentation().getAugmentationId();
      } else {
         this._isAugmented = false;
      }

      this._mana = item.getMana();
      this._time = item.isTimeLimitedItem() ? (int)(item.getRemainingTime() / 1000L) : -1;
      this._elemAtkType = item.getAttackElementType();
      this._elemAtkPower = item.getAttackElementPower();

      for (byte i = 0; i < 6; i++) {
         this._elemDefAttr[i] = item.getElementDefAttr(i);
      }

      this._enchantOptions = item.getEnchantOptions();
   }

   public ItemTemplate getItem() {
      return this._item;
   }

   public int getObjectId() {
      return this._object;
   }

   public int getOwnerId() {
      return this._owner;
   }

   public int getLocationSlot() {
      return this._locationSlot;
   }

   public long getCount() {
      return this._count;
   }

   public int getType1() {
      return this._item.getType1();
   }

   public int getType2() {
      return this._item.getType2();
   }

   public ItemType getItemType() {
      return this._item.getItemType();
   }

   public int getItemId() {
      return this._item.getId();
   }

   public BodyPart getBodyPart() {
      return this._item.getBodyPart();
   }

   public int getEnchantLevel() {
      return this._enchant;
   }

   public CrystalType getItemGrade() {
      return this._grade;
   }

   public boolean isWeapon() {
      return this._item instanceof Weapon;
   }

   public boolean isArmor() {
      return this._item instanceof Armor;
   }

   public boolean isEtcItem() {
      return this._item instanceof EtcItem;
   }

   public String getItemName() {
      return this._item.getName();
   }

   public boolean isAugmented() {
      return this._isAugmented;
   }

   public int getAugmentationId() {
      return this._augmentationId;
   }

   public String getName() {
      return this._item.getName();
   }

   public int getCustomType1() {
      return this._customType1;
   }

   public int getCustomType2() {
      return this._customType2;
   }

   public int getMana() {
      return this._mana;
   }

   public int getAttackElementType() {
      return this._elemAtkType;
   }

   public int getAttackElementPower() {
      return this._elemAtkPower;
   }

   public int getElementDefAttr(byte i) {
      return this._elemDefAttr[i];
   }

   public int[] getEnchantOptions() {
      return this._enchantOptions;
   }

   public int getTime() {
      return this._time;
   }

   @Override
   public String toString() {
      return this._item.toString();
   }
}
