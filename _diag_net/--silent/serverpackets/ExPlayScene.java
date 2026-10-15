package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExPlayScene extends ServerPacket {
   public static final ExPlayScene STATIC_PACKET = new ExPlayScene();

   private ExPlayScene() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PLAY_SCENE.writeId(this, buffer);
   }
}
