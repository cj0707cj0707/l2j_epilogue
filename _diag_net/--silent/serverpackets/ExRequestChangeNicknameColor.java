package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExRequestChangeNicknameColor extends ServerPacket {
   private final int _itemObjectId;

   public ExRequestChangeNicknameColor(int itemObjectId) {
      this._itemObjectId = itemObjectId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_REQUEST_CHANGE_NICKNAME_COLOR.writeId(this, buffer);
      buffer.writeInt(this._itemObjectId);
   }
}
