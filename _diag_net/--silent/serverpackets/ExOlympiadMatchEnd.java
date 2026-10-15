package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExOlympiadMatchEnd extends ServerPacket {
   public static final ExOlympiadMatchEnd STATIC_PACKET = new ExOlympiadMatchEnd();

   private ExOlympiadMatchEnd() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_OLYMPIAD_MATCH_END.writeId(this, buffer);
   }
}
