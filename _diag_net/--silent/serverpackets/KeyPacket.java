package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;

public class KeyPacket extends ServerPacket {
   private final byte[] _key;
   private final int _result;

   public KeyPacket(byte[] key, int result) {
      this._key = key;
      this._result = result;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.VERSION_CHECK.writeId(this, buffer);
      buffer.writeByte(this._result);

      for (int i = 0; i < 8; i++) {
         buffer.writeByte(this._key[i]);
      }

      buffer.writeInt(ServerConfig.PACKET_ENCRYPTION);
      buffer.writeInt(ServerConfig.SERVER_ID);
      buffer.writeByte(1);
      buffer.writeInt(0);
   }
}
