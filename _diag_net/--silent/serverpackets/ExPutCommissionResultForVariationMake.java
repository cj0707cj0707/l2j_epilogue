package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExPutCommissionResultForVariationMake extends ServerPacket {
   private final int _gemstoneObjId;
   private final int _itemId;
   private final long _gemstoneCount;
   private final int _unk1;
   private final int _unk2;
   private final int _unk3;

   public ExPutCommissionResultForVariationMake(int gemstoneObjId, long count, int itemId) {
      this._gemstoneObjId = gemstoneObjId;
      this._itemId = itemId;
      this._gemstoneCount = count;
      this._unk1 = 0;
      this._unk2 = 0;
      this._unk3 = 1;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PUT_COMMISSION_RESULT_FOR_VARIATION_MAKE.writeId(this, buffer);
      buffer.writeInt(this._gemstoneObjId);
      buffer.writeInt(this._itemId);
      buffer.writeLong(this._gemstoneCount);
      buffer.writeInt(this._unk1);
      buffer.writeInt(this._unk2);
      buffer.writeInt(this._unk3);
   }
}
