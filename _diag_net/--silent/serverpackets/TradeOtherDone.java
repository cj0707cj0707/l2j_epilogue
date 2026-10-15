package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class TradeOtherDone extends ServerPacket {
   public static final TradeOtherDone STATIC_PACKET = new TradeOtherDone();

   private TradeOtherDone() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.TRADE_PRESS_OTHER_OK.writeId(this, buffer);
   }
}
