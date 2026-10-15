package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExAskJoinMPCC extends ServerPacket {
   private final String _requestorName;

   public ExAskJoinMPCC(String requestorName) {
      this._requestorName = requestorName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_ASK_JOIN_MPCC.writeId(this, buffer);
      buffer.writeString(this._requestorName);
   }
}
