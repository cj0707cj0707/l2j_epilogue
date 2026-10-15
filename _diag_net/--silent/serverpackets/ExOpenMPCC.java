package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExOpenMPCC extends ServerPacket {
   public static final ExOpenMPCC STATIC_PACKET = new ExOpenMPCC();

   private ExOpenMPCC() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_OPEN_MPCC.writeId(this, buffer);
   }
}
