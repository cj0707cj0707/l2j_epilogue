package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SetSummonRemainTime extends ServerPacket {
   private final int _maxTime;
   private final int _remainingTime;

   public SetSummonRemainTime(int maxTime, int remainingTime) {
      this._remainingTime = remainingTime;
      this._maxTime = maxTime;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SET_SUMMON_REMAIN_TIME.writeId(this, buffer);
      buffer.writeInt(this._maxTime);
      buffer.writeInt(this._remainingTime);
   }
}
