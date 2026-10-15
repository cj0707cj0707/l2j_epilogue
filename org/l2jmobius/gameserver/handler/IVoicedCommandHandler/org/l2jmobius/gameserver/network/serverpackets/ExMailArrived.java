package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExMailArrived extends ServerPacket {
   public static final ExMailArrived STATIC_PACKET = new ExMailArrived();

   private ExMailArrived() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_MAIL_ARRIVED.writeId(this, buffer);
   }
}
