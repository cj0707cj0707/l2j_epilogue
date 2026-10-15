package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ExChooseInventoryAttributeItem extends ServerPacket {
   private final int _itemId;

   public ExChooseInventoryAttributeItem(Item item) {
      this._itemId = item.getDisplayId();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_CHOOSE_INVENTORY_ATTRIBUTE_ITEM.writeId(this, buffer);
      buffer.writeInt(this._itemId);
   }
}
