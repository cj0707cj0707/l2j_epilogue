package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExGetBossRecord extends ServerPacket {
   private final Map<Integer, Integer> _bossRecordInfo;
   private final int _ranking;
   private final int _totalPoints;

   public ExGetBossRecord(int ranking, int totalScore, Map<Integer, Integer> list) {
      this._ranking = ranking;
      this._totalPoints = totalScore;
      this._bossRecordInfo = list;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_GET_BOSS_RECORD.writeId(this, buffer);
      buffer.writeInt(this._ranking);
      buffer.writeInt(this._totalPoints);
      if (this._bossRecordInfo == null) {
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
      } else {
         buffer.writeInt(this._bossRecordInfo.size());

         for (Entry<Integer, Integer> entry : this._bossRecordInfo.entrySet()) {
            buffer.writeInt(entry.getKey());
            buffer.writeInt(entry.getValue());
            buffer.writeInt(0);
         }
      }
   }
}
