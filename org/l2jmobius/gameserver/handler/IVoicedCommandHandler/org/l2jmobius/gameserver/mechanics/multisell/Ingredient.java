package org.l2jmobius.gameserver.mechanics.multisell;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.util.StatSet;

public class Ingredient {
   private int _itemId;
   private long _itemCount;
   private final int _enchantmentLevel;
   private boolean _isTaxIngredient;
   private boolean _maintainIngredient;
   private ItemTemplate _template = null;
   private ItemInfo _itemInfo = null;

   public Ingredient(StatSet set) {
      this(
         set.getInt("id"),
         set.getLong("count"),
         set.getInt("enchantmentLevel", 0),
         set.getBoolean("isTaxIngredient", false),
         set.getBoolean("maintainIngredient", false)
      );
   }

   public Ingredient(int itemId, long itemCount, int enchantmentLevel, boolean isTaxIngredient, boolean maintainIngredient) {
      this._itemId = itemId;
      this._itemCount = itemCount;
      this._enchantmentLevel = enchantmentLevel;
      this._isTaxIngredient = isTaxIngredient;
      this._maintainIngredient = maintainIngredient;
      if (this._itemId > 0) {
         this._template = ItemData.getInstance().getTemplate(this._itemId);
      }
   }

   public Ingredient getCopy() {
      return new Ingredient(this._itemId, this._itemCount, this._enchantmentLevel, this._isTaxIngredient, this._maintainIngredient);
   }

   public ItemTemplate getTemplate() {
      return this._template;
   }

   public void setItemInfo(Item item) {
      this._itemInfo = new ItemInfo(item);
   }

   public void setItemInfo(ItemInfo info) {
      this._itemInfo = info;
   }

   public ItemInfo getItemInfo() {
      return this._itemInfo;
   }

   public int getEnchantLevel() {
      return this._itemInfo == null ? this._enchantmentLevel : this._itemInfo.getEnchantLevel();
   }

   public void setItemId(int itemId) {
      this._itemId = itemId;
   }

   public int getItemId() {
      return this._itemId;
   }

   public void setItemCount(long itemCount) {
      this._itemCount = itemCount;
   }

   public long getItemCount() {
      return this._itemCount;
   }

   public void setTaxIngredient(boolean isTaxIngredient) {
      this._isTaxIngredient = isTaxIngredient;
   }

   public boolean isTaxIngredient() {
      return this._isTaxIngredient;
   }

   public void setMaintainIngredient(boolean maintainIngredient) {
      this._maintainIngredient = maintainIngredient;
   }

   public boolean getMaintainIngredient() {
      return this._maintainIngredient;
   }

   public boolean isStackable() {
      return this._template == null ? true : this._template.isStackable();
   }

   public boolean isArmorOrWeapon() {
      return this._template == null ? false : this._template instanceof Armor || this._template instanceof Weapon;
   }

   public int getWeight() {
      return this._template == null ? 0 : this._template.getWeight();
   }
}
