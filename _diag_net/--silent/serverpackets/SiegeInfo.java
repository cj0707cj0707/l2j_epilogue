package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Calendar;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

public class SiegeInfo extends ServerPacket {
   private Castle _castle;
   private ClanHall _hall;
   private final Player _player;

   public SiegeInfo(Castle castle, Player player) {
      this._castle = castle;
      this._player = player;
   }

   public SiegeInfo(ClanHall hall, Player player) {
      this._hall = hall;
      this._player = player;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CASTLE_SIEGE_INFO.writeId(this, buffer);
      if (this._castle != null) {
         buffer.writeInt(this._castle.getResidenceId());
         int ownerId = this._castle.getOwnerId();
         buffer.writeInt(ownerId == this._player.getClanId() && this._player.isClanLeader());
         buffer.writeInt(ownerId);
         if (ownerId > 0) {
            Clan owner = ClanTable.getInstance().getClan(ownerId);
            if (owner != null) {
               buffer.writeString(owner.getName());
               buffer.writeString(owner.getLeaderName());
               buffer.writeInt(owner.getAllyId());
               buffer.writeString(owner.getAllyName());
            } else {
               org.l2jmobius.gameserver.network.PacketLogger.warning("Null owner for castle: " + this._castle.getName());
            }
         } else {
            buffer.writeString("");
            buffer.writeString("");
            buffer.writeInt(0);
            buffer.writeString("");
         }

         buffer.writeInt((int)(System.currentTimeMillis() / 1000L));
         if (!this._castle.isTimeRegistrationOver() && this._player.isClanLeader() && this._player.getClanId() == this._castle.getOwnerId()) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(this._castle.getSiegeDate().getTimeInMillis());
            cal.set(12, 0);
            cal.set(13, 0);
            buffer.writeInt(0);
            buffer.writeInt(FeatureConfig.SIEGE_HOUR_LIST.size());

            for (int hour : FeatureConfig.SIEGE_HOUR_LIST) {
               cal.set(11, hour);
               buffer.writeInt((int)(cal.getTimeInMillis() / 1000L));
            }
         } else {
            buffer.writeInt((int)(this._castle.getSiegeDate().getTimeInMillis() / 1000L));
            buffer.writeInt(0);
         }
      } else {
         buffer.writeInt(this._hall.getId());
         int ownerIdx = this._hall.getOwnerId();
         buffer.writeInt(ownerIdx == this._player.getClanId() && this._player.isClanLeader());
         buffer.writeInt(ownerIdx);
         if (ownerIdx > 0) {
            Clan owner = ClanTable.getInstance().getClan(ownerIdx);
            if (owner != null) {
               buffer.writeString(owner.getName());
               buffer.writeString(owner.getLeaderName());
               buffer.writeInt(owner.getAllyId());
               buffer.writeString(owner.getAllyName());
            } else {
               org.l2jmobius.gameserver.network.PacketLogger.warning("Null owner for siegable hall: " + this._hall.getName());
            }
         } else {
            buffer.writeString("");
            buffer.writeString("");
            buffer.writeInt(0);
            buffer.writeString("");
         }

         buffer.writeInt((int)(System.currentTimeMillis() / 1000L));
         buffer.writeInt((int)(CHSiegeManager.getInstance().getSiegableHall(this._hall.getId()).getNextSiegeTime() / 1000L));
         buffer.writeInt(0);
      }
   }
}
