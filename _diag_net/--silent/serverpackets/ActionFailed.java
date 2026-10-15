package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ActionFailed extends ServerPacket {
   public static final ActionFailed STATIC_PACKET = new ActionFailed();

   private ActionFailed() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ACTION_FAIL.writeId(this, buffer);
   }
}
