package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExRedSky extends ServerPacket {
   private final int _duration;

   public ExRedSky(int duration) {
      this._duration = duration;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_RED_SKY.writeId(this, buffer);
      buffer.writeInt(this._duration);
   }
}
