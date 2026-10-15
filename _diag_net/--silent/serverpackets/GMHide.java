package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class GMHide extends ServerPacket {
   private final int _mode;

   public GMHide(int mode) {
      this._mode = mode;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GM_HIDE.writeId(this, buffer);
      buffer.writeInt(this._mode);
   }
}
