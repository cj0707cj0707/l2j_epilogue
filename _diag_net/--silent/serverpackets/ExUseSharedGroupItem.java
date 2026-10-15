package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExUseSharedGroupItem extends ServerPacket {
   private final int _itemId;
   private final int _grpId;
   private final int _remainingTime;
   private final int _totalTime;

   public ExUseSharedGroupItem(int itemId, int grpId, long remainingTime, int totalTime) {
      this._itemId = itemId;
      this._grpId = grpId;
      this._remainingTime = (int)(remainingTime / 1000L);
      this._totalTime = totalTime / 1000;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_USE_SHARED_GROUP_ITEM.writeId(this, buffer);
      buffer.writeInt(this._itemId);
      buffer.writeInt(this._grpId);
      buffer.writeInt(this._remainingTime);
      buffer.writeInt(this._totalTime);
   }
}
