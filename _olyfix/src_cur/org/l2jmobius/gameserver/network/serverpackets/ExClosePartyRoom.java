package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExClosePartyRoom extends ServerPacket {
   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_CLOSE_PARTY_ROOM.writeId(this, buffer);
   }
}
