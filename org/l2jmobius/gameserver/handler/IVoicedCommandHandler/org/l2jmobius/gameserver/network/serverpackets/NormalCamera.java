package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class NormalCamera extends ServerPacket {
   public static final NormalCamera STATIC_PACKET = new NormalCamera();

   private NormalCamera() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.NORMAL_CAMERA.writeId(this, buffer);
   }
}
