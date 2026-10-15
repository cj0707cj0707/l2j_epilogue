package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SendTradeRequest extends ServerPacket {
   private final int _senderId;

   public SendTradeRequest(int senderId) {
      this._senderId = senderId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TRADE_REQUEST.writeId(this, buffer);
      buffer.writeInt(this._senderId);
   }
}
