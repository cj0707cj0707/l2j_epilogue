package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExAutoSoulShot extends ServerPacket {
   private final int _itemId;
   private final int _type;

   public ExAutoSoulShot(int itemId, int type) {
      this._itemId = itemId;
      this._type = type;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_AUTO_SOULSHOT.writeId(this, buffer);
      buffer.writeInt(this._itemId);
      buffer.writeInt(this._type);
   }
}
