package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class LeaveWorld extends ServerPacket {
   public static final LeaveWorld STATIC_PACKET = new LeaveWorld();

   private LeaveWorld() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.LOG_OUT_OK.writeId(this, buffer);
   }
}
