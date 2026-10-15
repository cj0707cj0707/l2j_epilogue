package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExSearchOrc extends ServerPacket {
   public static final ExSearchOrc STATIC_PACKET = new ExSearchOrc();

   private ExSearchOrc() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SEARCH_ORC.writeId(this, buffer);
   }
}
