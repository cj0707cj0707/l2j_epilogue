package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class DropItem extends ServerPacket {
   private final Item _item;
   private final int _objectId;

   public DropItem(Item item, int playerObjId) {
      this._item = item;
      this._objectId = playerObjId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.DROP_ITEM.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._item.getObjectId());
      buffer.writeInt(this._item.getDisplayId());
      buffer.writeInt(this._item.getX());
      buffer.writeInt(this._item.getY());
      buffer.writeInt(this._item.getZ());
      buffer.writeInt(this._item.isStackable());
      buffer.writeLong(this._item.getCount());
      buffer.writeInt(1);
   }
}
