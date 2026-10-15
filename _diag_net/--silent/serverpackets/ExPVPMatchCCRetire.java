package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExPVPMatchCCRetire extends ServerPacket {
   public static final ServerPacket STATIC = new ExPVPMatchCCRetire();

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PVP_MATCH_CC_RETIRE.writeId(this, buffer);
   }
}
