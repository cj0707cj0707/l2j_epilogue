package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class NetPing extends ServerPacket {
   private final int _time;

   public NetPing(int time) {
      this._time = time;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.NET_PING.writeId(this, buffer);
      buffer.writeInt(this._time);
   }
}
