package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartySmallWindowDeleteAll extends ServerPacket {
   public static final PartySmallWindowDeleteAll STATIC_PACKET = new PartySmallWindowDeleteAll();

   private PartySmallWindowDeleteAll() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PARTY_SMALL_WINDOW_DELETE_ALL.writeId(this, buffer);
   }
}
