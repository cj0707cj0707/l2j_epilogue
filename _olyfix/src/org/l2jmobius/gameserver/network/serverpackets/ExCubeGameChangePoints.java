package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExCubeGameChangePoints extends ServerPacket {
   private final int _timeLeft;
   private final int _bluePoints;
   private final int _redPoints;

   public ExCubeGameChangePoints(int timeLeft, int bluePoints, int redPoints) {
      this._timeLeft = timeLeft;
      this._bluePoints = bluePoints;
      this._redPoints = redPoints;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BLOCK_UP_SET_STATE.writeId(this, buffer);
      buffer.writeInt(2);
      buffer.writeInt(this._timeLeft);
      buffer.writeInt(this._bluePoints);
      buffer.writeInt(this._redPoints);
   }
}
