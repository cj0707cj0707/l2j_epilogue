package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.entity.item.enums.ElementalItemType;

public class ElementalItemHolder {
   private final int _itemId;
   private final byte _elementId;
   private final ElementalItemType _type;

   public ElementalItemHolder(int itemId, int elementId, ElementalItemType type) {
      this._itemId = itemId;
      this._elementId = (byte)elementId;
      this._type = type;
   }

   public int getItemId() {
      return this._itemId;
   }

   public byte getElementId() {
      return this._elementId;
   }

   public ElementalItemType getType() {
      return this._type;
   }
}
