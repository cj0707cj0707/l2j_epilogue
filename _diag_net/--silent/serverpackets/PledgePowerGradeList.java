package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan.RankPrivs;

public class PledgePowerGradeList extends ServerPacket {
   private final Collection<RankPrivs> _privs;

   public PledgePowerGradeList(Collection<RankPrivs> privs) {
      this._privs = privs;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_POWER_GRADE_LIST.writeId(this, buffer);
      buffer.writeInt(this._privs.size());

      for (RankPrivs temp : this._privs) {
         buffer.writeInt(temp.getRank());
         buffer.writeInt(temp.getParty());
      }
   }
}
