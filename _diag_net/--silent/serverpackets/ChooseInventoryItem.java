package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ChooseInventoryItem extends ServerPacket {
   private final int _itemId;

   public ChooseInventoryItem(int itemId) {
      this._itemId = itemId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHOOSE_INVENTORY_ITEM.writeId(this, buffer);
      buffer.writeInt(this._itemId);
   }
}
