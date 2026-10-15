package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExShowQuestInfo extends ServerPacket {
   public static final ExShowQuestInfo STATIC_PACKET = new ExShowQuestInfo();

   private ExShowQuestInfo() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_QUEST_INFO.writeId(this, buffer);
   }
}
