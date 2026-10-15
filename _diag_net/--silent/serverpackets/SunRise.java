package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SunRise extends ServerPacket {
   public static final SunRise STATIC_PACKET = new SunRise();

   private SunRise() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SUNRISE.writeId(this, buffer);
   }
}
