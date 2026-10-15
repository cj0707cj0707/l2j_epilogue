package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SendTradeRequest extends ServerPacket {
   private final int _senderId;

   public SendTradeRequest(int senderId) {
      this._senderId = senderId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.TRADE_REQUEST.writeId(this, buffer);
      buffer.writeInt(this._senderId);
   }
}
