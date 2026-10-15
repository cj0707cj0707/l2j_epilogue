package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class L2FriendSay extends ServerPacket {
   private final String _sender;
   private final String _receiver;
   private final String _message;

   public L2FriendSay(String sender, String receiver, String message) {
      this._sender = sender;
      this._receiver = receiver;
      this._message = message;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.L2_FRIEND_SAY.writeId(this, buffer);
      buffer.writeInt(0);
      buffer.writeString(this._receiver);
      buffer.writeString(this._sender);
      buffer.writeString(this._message);
   }
}
