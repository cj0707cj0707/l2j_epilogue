package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.TimeStamp;

public class SkillCoolTime extends ServerPacket {
   private final List<TimeStamp> _reuseTimestamps = new ArrayList<>();

   public SkillCoolTime(Player player) {
      for (TimeStamp ts : player.getSkillReuseTimeStamps().values()) {
         if (ts.hasNotPassed()) {
            this._reuseTimestamps.add(ts);
         }
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SKILL_COOL_TIME.writeId(this, buffer);
      buffer.writeInt(this._reuseTimestamps.size());

      for (TimeStamp ts : this._reuseTimestamps) {
         long reuse = ts.getReuse();
         long remaining = ts.getRemaining();
         buffer.writeInt(ts.getSkillId());
         buffer.writeInt(ts.getSkillLevel());
         buffer.writeInt((int)(reuse > 0L ? reuse : remaining) / 1000);
         buffer.writeInt(Math.max(1, (int)remaining / 1000));
      }
   }
}
