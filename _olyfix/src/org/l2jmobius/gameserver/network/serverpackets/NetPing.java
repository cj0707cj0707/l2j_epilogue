package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class NetPing extends ServerPacket {
   private final int _time;

   public NetPing(int time) {
      this._time = time;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.NET_PING.writeId(this, buffer);
      buffer.writeInt(this._time);
   }
}
