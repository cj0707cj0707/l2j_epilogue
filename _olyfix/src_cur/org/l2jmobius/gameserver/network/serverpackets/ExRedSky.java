package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExRedSky extends ServerPacket {
   private final int _duration;

   public ExRedSky(int duration) {
      this._duration = duration;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_RED_SKY.writeId(this, buffer);
      buffer.writeInt(this._duration);
   }
}
