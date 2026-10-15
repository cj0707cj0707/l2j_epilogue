package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SiegeDefenderList extends ServerPacket {
   private final Castle _castle;

   public SiegeDefenderList(Castle castle) {
      this._castle = castle;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CASTLE_SIEGE_DEFENDER_LIST.writeId(this, buffer);
      buffer.writeInt(this._castle.getResidenceId());
      buffer.writeInt(0);
      buffer.writeInt(1);
      buffer.writeInt(0);
      int size = this._castle.getSiege().getDefenderClans().size() + this._castle.getSiege().getDefenderWaitingClans().size();
      if (size > 0) {
         buffer.writeInt(size);
         buffer.writeInt(size);

         for (SiegeClan siegeclan : this._castle.getSiege().getDefenderClans()) {
            Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan != null) {
               buffer.writeInt(clan.getId());
               buffer.writeString(clan.getName());
               buffer.writeString(clan.getLeaderName());
               buffer.writeInt(clan.getCrestId());
               buffer.writeInt(0);
               switch (siegeclan.getType()) {
                  case OWNER:
                     buffer.writeInt(1);
                     break;
                  case DEFENDER_PENDING:
                     buffer.writeInt(2);
                     break;
                  case DEFENDER:
                     buffer.writeInt(3);
                     break;
                  default:
                     buffer.writeInt(0);
               }

               buffer.writeInt(clan.getAllyId());
               buffer.writeString(clan.getAllyName());
               buffer.writeString("");
               buffer.writeInt(clan.getAllyCrestId());
            }
         }

         for (SiegeClan siegeclanx : this._castle.getSiege().getDefenderWaitingClans()) {
            Clan clan = ClanTable.getInstance().getClan(siegeclanx.getClanId());
            buffer.writeInt(clan.getId());
            buffer.writeString(clan.getName());
            buffer.writeString(clan.getLeaderName());
            buffer.writeInt(clan.getCrestId());
            buffer.writeInt(0);
            buffer.writeInt(2);
            buffer.writeInt(clan.getAllyId());
            buffer.writeString(clan.getAllyName());
            buffer.writeString("");
            buffer.writeInt(clan.getAllyCrestId());
         }
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
      }
   }
}
