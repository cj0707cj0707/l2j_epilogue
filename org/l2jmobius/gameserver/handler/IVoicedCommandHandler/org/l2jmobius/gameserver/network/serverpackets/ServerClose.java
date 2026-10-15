package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ServerClose extends ServerPacket {
   public static final ServerClose STATIC_PACKET = new ServerClose();

   private ServerClose() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SERVER_CLOSE.writeId(this, buffer);
   }
}
