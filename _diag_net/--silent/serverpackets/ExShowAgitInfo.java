package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;

public class ExShowAgitInfo extends ServerPacket {
   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_AGIT_INFO.writeId(this, buffer);
      Map<Integer, AuctionableHall> clannhalls = ClanHallTable.getInstance().getAllAuctionableClanHalls();
      buffer.writeInt(clannhalls.size());

      for (AuctionableHall ch : clannhalls.values()) {
         buffer.writeInt(ch.getId());
         buffer.writeString(ch.getOwnerId() <= 0 ? "" : ClanTable.getInstance().getClan(ch.getOwnerId()).getName());
         buffer.writeString(ch.getOwnerId() <= 0 ? "" : ClanTable.getInstance().getClan(ch.getOwnerId()).getLeaderName());
         buffer.writeInt(ch.getGrade() < 1);
      }
   }
}
