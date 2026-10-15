package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ChooseInventoryItem extends ServerPacket {
   private final int _itemId;

   public ChooseInventoryItem(int itemId) {
      this._itemId = itemId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHOOSE_INVENTORY_ITEM.writeId(this, buffer);
      buffer.writeInt(this._itemId);
   }
}
