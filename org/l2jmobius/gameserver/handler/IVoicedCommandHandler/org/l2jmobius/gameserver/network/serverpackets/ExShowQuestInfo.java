package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowQuestInfo extends ServerPacket {
   public static final ExShowQuestInfo STATIC_PACKET = new ExShowQuestInfo();

   private ExShowQuestInfo() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_QUEST_INFO.writeId(this, buffer);
   }
}
