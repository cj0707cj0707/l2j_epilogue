package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExRequestHackShield extends ServerPacket {
   public static final ExRequestHackShield STATIC_PACKET = new ExRequestHackShield();

   private ExRequestHackShield() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_REQUEST_HACK_SHIELD.writeId(this, buffer);
   }
}
