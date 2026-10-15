package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SunRise extends ServerPacket {
   public static final SunRise STATIC_PACKET = new SunRise();

   private SunRise() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SUNRISE.writeId(this, buffer);
   }
}
