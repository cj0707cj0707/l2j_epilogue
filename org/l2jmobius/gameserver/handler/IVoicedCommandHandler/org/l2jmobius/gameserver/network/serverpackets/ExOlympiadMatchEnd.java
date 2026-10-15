package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExOlympiadMatchEnd extends ServerPacket {
   public static final ExOlympiadMatchEnd STATIC_PACKET = new ExOlympiadMatchEnd();

   private ExOlympiadMatchEnd() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_OLYMPIAD_MATCH_END.writeId(this, buffer);
   }
}
