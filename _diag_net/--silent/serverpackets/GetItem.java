package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class GetItem extends ServerPacket {
   private final Item _item;
   private final int _playerId;

   public GetItem(Item item, int playerId) {
      this._item = item;
      this._playerId = playerId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GET_ITEM.writeId(this, buffer);
      buffer.writeInt(this._playerId);
      buffer.writeInt(this._item.getObjectId());
      buffer.writeInt(this._item.getX());
      buffer.writeInt(this._item.getY());
      buffer.writeInt(this._item.getZ());
   }
}
