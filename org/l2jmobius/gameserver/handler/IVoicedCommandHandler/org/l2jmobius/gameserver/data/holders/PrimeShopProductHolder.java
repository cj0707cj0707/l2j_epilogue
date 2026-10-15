package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;

public class PrimeShopProductHolder {
   private final int _productId;
   private final int _category;
   private final int _points;
   private final int _item;
   private final int _count;
   private final int _weight;
   private final boolean _tradable;

   public PrimeShopProductHolder(int productId, int category, int points, int item, int count) {
      this._productId = productId;
      this._category = category;
      this._points = points;
      this._item = item;
      this._count = count;
      ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(item);
      if (itemTemplate != null) {
         this._weight = itemTemplate.getWeight();
         this._tradable = itemTemplate.isTradeable();
      } else {
         this._weight = 0;
         this._tradable = true;
      }
   }

   public int getProductId() {
      return this._productId;
   }

   public int getCategory() {
      return this._category;
   }

   public int getPrice() {
      return this._points;
   }

   public int getItemId() {
      return this._item;
   }

   public int getItemCount() {
      return this._count;
   }

   public int getItemWeight() {
      return this._weight;
   }

   public boolean isTradable() {
      return this._tradable;
   }
}
