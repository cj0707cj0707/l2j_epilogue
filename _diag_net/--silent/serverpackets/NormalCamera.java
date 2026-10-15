package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class NormalCamera extends ServerPacket {
   public static final NormalCamera STATIC_PACKET = new NormalCamera();

   private NormalCamera() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.NORMAL_CAMERA.writeId(this, buffer);
   }
}
