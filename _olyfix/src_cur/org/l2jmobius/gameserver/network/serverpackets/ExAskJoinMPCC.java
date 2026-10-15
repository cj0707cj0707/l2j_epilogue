package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExAskJoinMPCC extends ServerPacket {
   private final String _requestorName;

   public ExAskJoinMPCC(String requestorName) {
      this._requestorName = requestorName;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_ASK_JOIN_MPCC.writeId(this, buffer);
      buffer.writeString(this._requestorName);
   }
}
