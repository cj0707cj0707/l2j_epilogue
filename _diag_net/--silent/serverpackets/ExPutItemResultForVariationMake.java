package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExPutItemResultForVariationMake extends ServerPacket {
   private final int _itemObjId;
   private final int _itemId;

   public ExPutItemResultForVariationMake(int itemObjId, int itemId) {
      this._itemObjId = itemObjId;
      this._itemId = itemId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PUT_ITEM_RESULT_FOR_VARIATION_MAKE.writeId(this, buffer);
      buffer.writeInt(this._itemObjId);
      buffer.writeInt(this._itemId);
      buffer.writeInt(1);
   }
}
