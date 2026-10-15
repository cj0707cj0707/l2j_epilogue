package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExRequestHackShield extends ServerPacket {
   public static final ExRequestHackShield STATIC_PACKET = new ExRequestHackShield();

   private ExRequestHackShield() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_REQUEST_HACK_SHIELD.writeId(this, buffer);
   }
}
