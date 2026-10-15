package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;

public class SiegeAttackerList extends ServerPacket {
   private Castle _castle;
   private SiegableHall _hall;

   public SiegeAttackerList(Castle castle) {
      this._castle = castle;
   }

   public SiegeAttackerList(SiegableHall hall) {
      this._hall = hall;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CASTLE_SIEGE_ATTACKER_LIST.writeId(this, buffer);
      if (this._castle != null) {
         buffer.writeInt(this._castle.getResidenceId());
         buffer.writeInt(0);
         buffer.writeInt(1);
         buffer.writeInt(0);
         int size = this._castle.getSiege().getAttackerClans().size();
         if (size > 0) {
            buffer.writeInt(size);
            buffer.writeInt(size);

            for (SiegeClan siegeclan : this._castle.getSiege().getAttackerClans()) {
               Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
               if (clan != null) {
                  buffer.writeInt(clan.getId());
                  buffer.writeString(clan.getName());
                  buffer.writeString(clan.getLeaderName());
                  buffer.writeInt(clan.getCrestId());
                  buffer.writeInt(0);
                  buffer.writeInt(clan.getAllyId());
                  buffer.writeString(clan.getAllyName());
                  buffer.writeString("");
                  buffer.writeInt(clan.getAllyCrestId());
               }
            }
         } else {
            buffer.writeInt(0);
            buffer.writeInt(0);
         }
      } else {
         buffer.writeInt(this._hall.getId());
         buffer.writeInt(0);
         buffer.writeInt(1);
         buffer.writeInt(0);
         Collection<SiegeClan> attackers = this._hall.getSiege().getAttackerClans();
         int size = attackers.size();
         if (size > 0) {
            buffer.writeInt(size);
            buffer.writeInt(size);

            for (SiegeClan sClan : attackers) {
               Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
               if (clan != null) {
                  buffer.writeInt(clan.getId());
                  buffer.writeString(clan.getName());
                  buffer.writeString(clan.getLeaderName());
                  buffer.writeInt(clan.getCrestId());
                  buffer.writeInt(0);
                  buffer.writeInt(clan.getAllyId());
                  buffer.writeString(clan.getAllyName());
                  buffer.writeString("");
                  buffer.writeInt(clan.getAllyCrestId());
               }
            }
         } else {
            buffer.writeInt(0);
            buffer.writeInt(0);
         }
      }
   }
}
