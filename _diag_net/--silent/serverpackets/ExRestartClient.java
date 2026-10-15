package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExRestartClient extends ServerPacket {
   public static final ExRestartClient STATIC_PACKET = new ExRestartClient();

   private ExRestartClient() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_RESTART_CLIENT.writeId(this, buffer);
   }
}
