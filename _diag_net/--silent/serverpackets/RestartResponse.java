package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

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
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.RESTART_RESPONSE.writeId(this, buffer);
      buffer.writeInt(this._result);
   }
}
