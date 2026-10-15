package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExRegenMax extends ServerPacket {
   private final int _time;
   private final int _tickInterval;
   private final double _amountPerTick;

   public ExRegenMax(int time, int tickInterval, double amountPerTick) {
      this._time = time;
      this._tickInterval = tickInterval;
      this._amountPerTick = amountPerTick;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_REGEN_MAX.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._tickInterval);
      buffer.writeInt(this._time);
      buffer.writeDouble(this._amountPerTick);
   }
}
