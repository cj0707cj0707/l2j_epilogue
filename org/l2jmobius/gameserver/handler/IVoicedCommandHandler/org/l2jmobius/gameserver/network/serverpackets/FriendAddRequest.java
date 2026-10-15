package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class FriendAddRequest extends ServerPacket {
   private final String _requestorName;

   public FriendAddRequest(String requestorName) {
      this._requestorName = requestorName;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.FRIEND_ADD_REQUEST.writeId(this, buffer);
      buffer.writeString(this._requestorName);
      buffer.writeInt(0);
   }
}
