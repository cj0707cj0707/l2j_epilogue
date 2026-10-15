package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.entity.item.instance.Item;

public class PetItemHolder {
   private final Item _item;

   public PetItemHolder(Item item) {
      this._item = item;
   }

   public Item getItem() {
      return this._item;
   }
}
