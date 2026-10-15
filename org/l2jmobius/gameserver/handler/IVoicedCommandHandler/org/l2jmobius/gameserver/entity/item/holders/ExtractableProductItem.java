package org.l2jmobius.gameserver.entity.item.holders;

import java.util.List;

public class ExtractableProductItem {
   private final List<RestorationItemHolder> _items;
   private final double _chance;

   public ExtractableProductItem(List<RestorationItemHolder> items, double chance) {
      this._items = items;
      this._chance = chance;
   }

   public List<RestorationItemHolder> getItems() {
      return this._items;
   }

   public double getChance() {
      return this._chance;
   }
}
