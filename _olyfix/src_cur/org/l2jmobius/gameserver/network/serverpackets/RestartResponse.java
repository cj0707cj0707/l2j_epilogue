package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class RestartResponse extends ServerPacket {
   private static final RestartResponse STATIC_PACKET_TRUE = new RestartResponse(true);
   private static final RestartResponse STATIC_PACKET_FALSE = new RestartResponse(false);
   private final boolean _result;

   public static RestartResponse valueOf(boolean result) {
      return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
   }

   public RestartResponse(boolean result) {
      this._result = result;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.RESTART_RESPONSE.writeId(this, buffer);
      buffer.writeInt(this._result);
   }
}
