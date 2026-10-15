package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExCubeGameChangeTimeToStart extends ServerPacket {
   private final int _seconds;

   public ExCubeGameChangeTimeToStart(int seconds) {
      this._seconds = seconds;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_LIST.writeId(this, buffer);
      buffer.writeInt(3);
      buffer.writeInt(this._seconds);
   }
}
