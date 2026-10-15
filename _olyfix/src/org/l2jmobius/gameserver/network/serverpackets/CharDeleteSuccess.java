package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharDeleteSuccess extends ServerPacket {
   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHARACTER_DELETE_SUCCESS.writeId(this, buffer);
   }
}
