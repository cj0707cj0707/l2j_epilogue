package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.network.serverpackets.ExShowDominionRegistry;

public class RequestJoinDominionWar extends ClientPacket {
   private int _territoryId;
   private int _isClan;
   private int _isJoining;

   @Override
   protected void readImpl() {
      this._territoryId = this.readInt();
      this._isClan = this.readInt();
      this._isJoining = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (TerritoryWarManager.getInstance().isRegistrationOver()) {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.IT_IS_NOT_A_TERRITORY_WAR_REGISTRATION_PERIOD_SO_A_REQUEST_CANNOT_BE_MADE_AT_THIS_TIME
            );
         } else {
            Clan clan = player.getClan();
            int castleId = this._territoryId - 80;
            if (clan != null && TerritoryWarManager.getInstance().getTerritory(castleId).getOwnerClan() == clan) {
               player.sendPacket(
                  org.l2jmobius.gameserver.network.SystemMessageId.THE_CLAN_WHO_OWNS_THE_TERRITORY_CANNOT_PARTICIPATE_IN_THE_TERRITORY_WAR_AS_MERCENARIES
               );
            } else {
               if (this._isClan == 1) {
                  if (!player.hasAccess(ClanAccess.CASTLE_SIEGE)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
                     return;
                  }

                  if (clan == null) {
                     return;
                  }

                  if (this._isJoining == 1) {
                     if (System.currentTimeMillis() < clan.getDissolvingExpiryTime()) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOUR_CLAN_MAY_NOT_REGISTER_TO_PARTICIPATE_IN_A_SIEGE_WHILE_UNDER_A_GRACE_PERIOD_OF_THE_CLAN_S_DISSOLUTION
                        );
                        return;
                     }

                     if (TerritoryWarManager.getInstance().checkIsRegistered(-1, clan)) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_VE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE
                        );
                        return;
                     }

                     TerritoryWarManager.getInstance().registerClan(castleId, clan);
                  } else {
                     TerritoryWarManager.getInstance().removeClan(castleId, clan);
                  }
               } else {
                  if (player.getLevel() < 40 || player.getPlayerClass().level() < 2) {
                     return;
                  }

                  if (this._isJoining == 1) {
                     if (TerritoryWarManager.getInstance().checkIsRegistered(-1, player.getObjectId())) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_VE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE
                        );
                        return;
                     }

                     if (clan != null && TerritoryWarManager.getInstance().checkIsRegistered(-1, clan)) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_VE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE
                        );
                        return;
                     }

                     TerritoryWarManager.getInstance().registerMerc(castleId, player);
                  } else {
                     TerritoryWarManager.getInstance().removeMerc(castleId, player);
                  }
               }

               player.sendPacket(new ExShowDominionRegistry(castleId, player));
            }
         }
      }
   }
}
