package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class AskJoinAlly extends ServerPacket {
   private final String _requestorName;
   private final int _requestorObjId;

   public AskJoinAlly(int requestorObjId, String requestorName) {
      this._requestorName = requestorName;
      this._requestorObjId = requestorObjId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.ASK_JOIN_ALLIANCE.writeId(this, buffer);
      buffer.writeInt(this._requestorObjId);
      buffer.writeString(this._requestorName);
   }
}
