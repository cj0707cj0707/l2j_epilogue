package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ActionFailed extends ServerPacket {
   public static final ActionFailed STATIC_PACKET = new ActionFailed();

   private ActionFailed() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.ACTION_FAIL.writeId(this, buffer);
   }
}
