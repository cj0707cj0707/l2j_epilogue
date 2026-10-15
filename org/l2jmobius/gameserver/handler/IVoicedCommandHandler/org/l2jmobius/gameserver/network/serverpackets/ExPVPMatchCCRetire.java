package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPVPMatchCCRetire extends ServerPacket {
   public static final ServerPacket STATIC = new ExPVPMatchCCRetire();

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PVP_MATCH_CC_RETIRE.writeId(this, buffer);
   }
}
