package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class FriendAddRequest extends ServerPacket {
   private final String _requestorName;

   public FriendAddRequest(String requestorName) {
      this._requestorName = requestorName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.FRIEND_ADD_REQUEST.writeId(this, buffer);
      buffer.writeString(this._requestorName);
      buffer.writeInt(0);
   }
}
