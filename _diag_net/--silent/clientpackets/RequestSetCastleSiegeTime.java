package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Calendar;
import java.util.Date;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestSetCastleSiegeTime extends ClientPacket {
   private int _castleId;
   private long _time;

   @Override
   protected void readImpl() {
      this._castleId = this.readInt();
      this._time = this.readInt();
      this._time *= 1000L;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      Castle castle = CastleManager.getInstance().getCastleById(this._castleId);
      if (player != null && castle != null) {
         if (castle.getOwnerId() > 0 && castle.getOwnerId() != player.getClanId()) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "[C]RequestSetCastleSiegeTime: activeChar: "
                  + player
                  + " castle: "
                  + castle
                  + " castleId: "
                  + this._castleId
                  + " is trying to change siege date of not his own castle!"
            );
         } else if (!player.isClanLeader()) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "[C]RequestSetCastleSiegeTime: activeChar: "
                  + player
                  + " castle: "
                  + castle
                  + " castleId: "
                  + this._castleId
                  + " is trying to change siege date but is not clan leader!"
            );
         } else if (!castle.isTimeRegistrationOver()) {
            if (isSiegeTimeValid(castle.getSiegeDate().getTimeInMillis(), this._time)) {
               castle.getSiegeDate().setTimeInMillis(this._time);
               castle.setTimeRegistrationOver(true);
               castle.getSiege().saveSiegeDate();
               SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_ANNOUNCED_THE_CASTLE_SIEGE_TIME);
               msg.addCastleId(this._castleId);
               World.broadcastToAllOnlinePlayers(msg);
               player.sendPacket(new SiegeInfo(castle, player));
            } else {
               org.l2jmobius.gameserver.network.PacketLogger.warning(
                  "[C]RequestSetCastleSiegeTime: activeChar: "
                     + player
                     + " castle: "
                     + castle
                     + " castleId: "
                     + this._castleId
                     + " is trying to an invalid time ("
                     + new Date(this._time)
                     + " !"
               );
            }
         } else {
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "[C]RequestSetCastleSiegeTime: activeChar: "
                  + player
                  + " castle: "
                  + castle
                  + " castleId: "
                  + this._castleId
                  + " is trying to change siege date but currently not possible!"
            );
         }
      } else {
         org.l2jmobius.gameserver.network.PacketLogger.warning(
            "[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + this._castleId
         );
      }
   }

   private static boolean isSiegeTimeValid(long siegeDate, long choosenDate) {
      Calendar cal1 = Calendar.getInstance();
      cal1.setTimeInMillis(siegeDate);
      cal1.set(12, 0);
      cal1.set(13, 0);
      Calendar cal2 = Calendar.getInstance();
      cal2.setTimeInMillis(choosenDate);

      for (int hour : FeatureConfig.SIEGE_HOUR_LIST) {
         cal1.set(11, hour);
         if (isEqual(cal1, cal2, 1, 2, 5, 10, 12, 13)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isEqual(Calendar cal1, Calendar cal2, int... fields) {
      for (int field : fields) {
         if (cal1.get(field) != cal2.get(field)) {
            return false;
         }
      }

      return true;
   }
}
