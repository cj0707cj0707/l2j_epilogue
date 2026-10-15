package org.l2jmobius.gameserver.entity.item.recipe;

import org.l2jmobius.gameserver.entity.item.instance.Item;

public class RecipeItemInfo {
   private final int _itemId;
   private int _quantity;
   private final int _referencePrice;
   private final String _itemName;

   public RecipeItemInfo(Item item, int quantity) {
      this._itemId = item.getId();
      this._quantity = quantity;
      this._itemName = item.getTemplate().getName();
      this._referencePrice = item.getReferencePrice();
   }

   public int getQuantity() {
      return this._quantity;
   }

   public void setQuantity(int quantity) {
      this._quantity = quantity;
   }

   public int getReferencePrice() {
      return this._referencePrice;
   }

   public int getItemId() {
      return this._itemId;
   }

   public String getItemName() {
      return this._itemName;
   }
}
