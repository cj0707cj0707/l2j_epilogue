package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowAdventurerGuideBook extends ServerPacket {
   public static final ExShowAdventurerGuideBook STATIC_PACKET = new ExShowAdventurerGuideBook();

   private ExShowAdventurerGuideBook() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_ADVENTURER_GUIDE_BOOK.writeId(this, buffer);
   }
}
