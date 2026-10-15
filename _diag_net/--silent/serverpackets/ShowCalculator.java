package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ShowCalculator extends ServerPacket {
   private final int _calculatorId;

   public ShowCalculator(int calculatorId) {
      this._calculatorId = calculatorId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHOW_CALC.writeId(this, buffer);
      buffer.writeInt(this._calculatorId);
   }
}
