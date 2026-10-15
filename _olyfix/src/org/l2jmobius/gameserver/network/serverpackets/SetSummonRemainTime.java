package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SetSummonRemainTime extends ServerPacket {
   private final int _maxTime;
   private final int _remainingTime;

   public SetSummonRemainTime(int maxTime, int remainingTime) {
      this._remainingTime = remainingTime;
      this._maxTime = maxTime;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SET_SUMMON_REMAIN_TIME.writeId(this, buffer);
      buffer.writeInt(this._maxTime);
      buffer.writeInt(this._remainingTime);
   }
}
