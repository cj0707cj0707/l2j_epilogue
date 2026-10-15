package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PartySmallWindowDeleteAll extends ServerPacket {
   public static final PartySmallWindowDeleteAll STATIC_PACKET = new PartySmallWindowDeleteAll();

   private PartySmallWindowDeleteAll() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PARTY_SMALL_WINDOW_DELETE_ALL.writeId(this, buffer);
   }
}
