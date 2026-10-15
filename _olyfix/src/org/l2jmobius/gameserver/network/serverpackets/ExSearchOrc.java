package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExSearchOrc extends ServerPacket {
   public static final ExSearchOrc STATIC_PACKET = new ExSearchOrc();

   private ExSearchOrc() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SEARCH_ORC.writeId(this, buffer);
   }
}
