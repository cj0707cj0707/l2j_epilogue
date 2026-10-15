package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CameraMode extends ServerPacket {
   private final int _mode;

   public CameraMode(int mode) {
      this._mode = mode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CAMERA_MODE.writeId(this, buffer);
      buffer.writeInt(this._mode);
   }
}
