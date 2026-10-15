package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class TradeOtherDone extends ServerPacket {
   public static final TradeOtherDone STATIC_PACKET = new TradeOtherDone();

   private TradeOtherDone() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TRADE_PRESS_OTHER_OK.writeId(this, buffer);
   }
}
