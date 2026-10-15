package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExOlympiadMode extends ServerPacket {
   private final int _mode;

   public ExOlympiadMode(int mode) {
      this._mode = mode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_OLYMPIAD_MODE.writeId(this, buffer);
      buffer.writeByte(this._mode);
   }
}
