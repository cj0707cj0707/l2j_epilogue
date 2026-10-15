package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class ItemInfo {
   private int _objectId;
   private ItemTemplate _item;
   private int _enchant;
   private int _augmentation;
   private long _count;
   private int _price;
   private int _type1;
   private int _type2;
   private int _equipped;
   private int _change;
   private int _mana;
   private int _time;
   private int _location;
   private int _elemAtkType = -2;
   private int _elemAtkPower = 0;
   private final int[] _elemDefAttr = new int[]{0, 0, 0, 0, 0, 0};
   private int[] _option;

   public ItemInfo(Item item) {
      if (item != null) {
         this._objectId = item.getObjectId();
         this._item = item.getTemplate();
         this._enchant = item.getEnchantLevel();
         this._augmentation = item.isAugmented() ? item.getAugmentation().getAugmentationId() : 0;
         this._count = item.getCount();
         this._type1 = item.getCustomType1();
         this._type2 = item.getCustomType2();
         this._equipped = item.isEquipped() ? 1 : 0;
         switch (item.getLastChange()) {
            case 1:
               this._change = 1;
               break;
            case 2:
               this._change = 2;
               break;
            case 3:
               this._change = 3;
         }

         this._mana = item.getMana();
         this._time = item.isTimeLimitedItem() ? (int)(item.getRemainingTime() / 1000L) : -9999;
         this._location = item.getLocationSlot();
         this._elemAtkType = item.getAttackElementType();
         this._elemAtkPower = item.getAttackElementPower();

         for (byte i = 0; i < 6; i++) {
            this._elemDefAttr[i] = item.getElementDefAttr(i);
         }

         this._option = item.getEnchantOptions();
      }
   }

   public ItemInfo(Item item, int change) {
      this(item);
      this._change = change;
   }

   public ItemInfo(TradeItem item) {
      if (item != null) {
         this._objectId = item.getObjectId();
         this._item = item.getItem();
         this._enchant = item.getEnchant();
         this._augmentation = 0;
         this._count = item.getCount();
         this._type1 = item.getCustomType1();
         this._type2 = item.getCustomType2();
         this._equipped = 0;
         this._change = 0;
         this._mana = -1;
         this._time = -9999;
         this._location = item.getLocationSlot();
         this._elemAtkType = item.getAttackElementType();
         this._elemAtkPower = item.getAttackElementPower();

         for (byte i = 0; i < 6; i++) {
            this._elemDefAttr[i] = item.getElementDefAttr(i);
         }

         this._option = item.getEnchantOptions();
      }
   }

   public int getObjectId() {
      return this._objectId;
   }

   public ItemTemplate getItem() {
      return this._item;
   }

   public int getEnchant() {
      return this._enchant;
   }

   public int getAugmentationBonus() {
      return this._augmentation;
   }

   public long getCount() {
      return this._count;
   }

   public int getPrice() {
      return this._price;
   }

   public int getCustomType1() {
      return this._type1;
   }

   public int getCustomType2() {
      return this._type2;
   }

   public int getEquipped() {
      return this._equipped;
   }

   public int getChange() {
      return this._change;
   }

   public int getMana() {
      return this._mana;
   }

   public int getTime() {
      return this._time;
   }

   public int getLocation() {
      return this._location;
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
      return this._option;
   }
}
