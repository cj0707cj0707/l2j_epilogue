package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExEventMatchMessage extends ServerPacket {
   private final int _type;
   private final String _message;

   public ExEventMatchMessage(int type, String message) {
      this._type = type;
      this._message = message;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_EVENT_MATCH_MESSAGE.writeId(this, buffer);
      buffer.writeByte(this._type);
      buffer.writeString(this._message);
   }
}
