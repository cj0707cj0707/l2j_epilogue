package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExPutIntensiveResultForVariationMake extends ServerPacket {
   private final int _refinerItemObjId;
   private final int _lifestoneItemId;
   private final int _gemstoneItemId;
   private final int _gemstoneCount;
   private final int _unk2;

   public ExPutIntensiveResultForVariationMake(int refinerItemObjId, int lifeStoneId, int gemstoneItemId, int gemstoneCount) {
      this._refinerItemObjId = refinerItemObjId;
      this._lifestoneItemId = lifeStoneId;
      this._gemstoneItemId = gemstoneItemId;
      this._gemstoneCount = gemstoneCount;
      this._unk2 = 1;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PUT_INTENSIVE_RESULT_FOR_VARIATION_MAKE.writeId(this, buffer);
      buffer.writeInt(this._refinerItemObjId);
      buffer.writeInt(this._lifestoneItemId);
      buffer.writeInt(this._gemstoneItemId);
      buffer.writeLong(this._gemstoneCount);
      buffer.writeInt(this._unk2);
   }
}
