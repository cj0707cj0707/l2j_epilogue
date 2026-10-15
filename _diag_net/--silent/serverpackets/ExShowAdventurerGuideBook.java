package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExShowAdventurerGuideBook extends ServerPacket {
   public static final ExShowAdventurerGuideBook STATIC_PACKET = new ExShowAdventurerGuideBook();

   private ExShowAdventurerGuideBook() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_ADVENTURER_GUIDE_BOOK.writeId(this, buffer);
   }
}
