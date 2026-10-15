package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class CharDeleteSuccess extends ServerPacket {
   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHARACTER_DELETE_SUCCESS.writeId(this, buffer);
   }
}
