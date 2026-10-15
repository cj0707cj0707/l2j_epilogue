package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExBrLoadEventTopRankers extends ServerPacket {
   private final int _eventId;
   private final int _day;
   private final int _count;
   private final int _bestScore;
   private final int _myScore;

   public ExBrLoadEventTopRankers(int eventId, int day, int count, int bestScore, int myScore) {
      this._eventId = eventId;
      this._day = day;
      this._count = count;
      this._bestScore = bestScore;
      this._myScore = myScore;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BR_LOAD_EVENT_TOP_RANKERS.writeId(this, buffer);
      buffer.writeInt(this._eventId);
      buffer.writeInt(this._day);
      buffer.writeInt(this._count);
      buffer.writeInt(this._bestScore);
      buffer.writeInt(this._myScore);
   }
}
