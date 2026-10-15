package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgePowerGradeList extends ServerPacket {
   private final Collection<Clan.RankPrivs> _privs;

   public PledgePowerGradeList(Collection<Clan.RankPrivs> privs) {
      this._privs = privs;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_POWER_GRADE_LIST.writeId(this, buffer);
      buffer.writeInt(this._privs.size());

      for (Clan.RankPrivs temp : this._privs) {
         buffer.writeInt(temp.getRank());
         buffer.writeInt(temp.getParty());
      }
   }
}
