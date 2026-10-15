package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class CameraMode extends ServerPacket {
   private final int _mode;

   public CameraMode(int mode) {
      this._mode = mode;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CAMERA_MODE.writeId(this, buffer);
      buffer.writeInt(this._mode);
   }
}
