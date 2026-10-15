package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExMailArrived extends ServerPacket {
   public static final ExMailArrived STATIC_PACKET = new ExMailArrived();

   private ExMailArrived() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_MAIL_ARRIVED.writeId(this, buffer);
   }
}
