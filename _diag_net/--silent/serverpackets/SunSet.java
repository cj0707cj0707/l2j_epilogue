package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SunSet extends ServerPacket {
   public static final SunSet STATIC_PACKET = new SunSet();

   private SunSet() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SUNSET.writeId(this, buffer);
   }
}
