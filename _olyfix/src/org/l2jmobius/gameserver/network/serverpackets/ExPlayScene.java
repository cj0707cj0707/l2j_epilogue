package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPlayScene extends ServerPacket {
   public static final ExPlayScene STATIC_PACKET = new ExPlayScene();

   private ExPlayScene() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PLAY_SCENE.writeId(this, buffer);
   }
}
