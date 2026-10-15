package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class AllyDismiss extends ClientPacket {
   private String _clanName;

   @Override
   protected void readImpl() {
      this._clanName = this.readString();
   }

   @Override
   protected void runImpl() {
      if (this._clanName != null) {
         Player player = this.getPlayer();
         if (player != null) {
            if (player.getClan() == null) {
               player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
            } else {
               Clan leaderClan = player.getClan();
               if (leaderClan.getAllyId() == 0) {
                  player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS);
               } else if (player.isClanLeader() && leaderClan.getId() == leaderClan.getAllyId()) {
                  Clan clan = ClanTable.getInstance().getClanByName(this._clanName);
                  if (clan == null) {
                     player.sendPacket(SystemMessageId.THAT_CLAN_DOES_NOT_EXIST);
                  } else if (clan.getId() == leaderClan.getId()) {
                     player.sendPacket(SystemMessageId.ALLIANCE_LEADERS_CANNOT_WITHDRAW);
                  } else if (clan.getAllyId() != leaderClan.getAllyId()) {
                     player.sendPacket(SystemMessageId.DIFFERENT_ALLIANCE);
                  } else {
                     long currentTime = System.currentTimeMillis();
                     leaderClan.setAllyPenaltyExpiryTime(currentTime + PlayerConfig.ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED * 86400000, 3);
                     leaderClan.updateClanInDB();
                     clan.setAllyId(0);
                     clan.setAllyName(null);
                     clan.changeAllyCrest(0, true);
                     clan.setAllyPenaltyExpiryTime(currentTime + PlayerConfig.ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED * 86400000, 2);
                     clan.updateClanInDB();
                     player.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_EXPELLING_A_CLAN);
                  }
               } else {
                  player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_ALLIANCE_LEADERS);
               }
            }
         }
      }
   }
}
