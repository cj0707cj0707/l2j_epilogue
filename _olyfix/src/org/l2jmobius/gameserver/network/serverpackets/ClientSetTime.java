package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class ClientSetTime extends ServerPacket {
   public static final ClientSetTime STATIC_PACKET = new ClientSetTime();

   private ClientSetTime() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CLIENT_SET_TIME.writeId(this, buffer);
      buffer.writeInt(GameTimeTaskManager.getInstance().getGameTime());
      buffer.writeInt(6);
   }
}
