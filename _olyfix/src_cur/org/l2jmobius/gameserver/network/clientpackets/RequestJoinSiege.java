package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;

public class RequestJoinSiege extends ClientPacket {
   private int _castleId;
   private int _isAttacker;
   private int _isJoining;

   @Override
   protected void readImpl() {
      this._castleId = this.readInt();
      this._isAttacker = this.readInt();
      this._isJoining = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.hasAccess(ClanAccess.CASTLE_SIEGE)) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         } else {
            Clan clan = player.getClan();
            if (clan != null) {
               Castle castle = CastleManager.getInstance().getCastleById(this._castleId);
               if (castle != null) {
                  if (this._isJoining == 1) {
                     if (System.currentTimeMillis() < clan.getDissolvingExpiryTime()) {
                        player.sendPacket(
                           SystemMessageId.YOUR_CLAN_MAY_NOT_REGISTER_TO_PARTICIPATE_IN_A_SIEGE_WHILE_UNDER_A_GRACE_PERIOD_OF_THE_CLAN_S_DISSOLUTION
                        );
                        return;
                     }

                     if (this._isAttacker == 1) {
                        castle.getSiege().registerAttacker(player);
                     } else {
                        castle.getSiege().registerDefender(player);
                     }
                  } else {
                     castle.getSiege().removeSiegeClan(player);
                  }

                  castle.getSiege().listRegisterClan(player);
               }

               SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(this._castleId);
               if (hall != null) {
                  if (this._isJoining == 1) {
                     if (System.currentTimeMillis() < clan.getDissolvingExpiryTime()) {
                        player.sendPacket(
                           SystemMessageId.YOUR_CLAN_MAY_NOT_REGISTER_TO_PARTICIPATE_IN_A_SIEGE_WHILE_UNDER_A_GRACE_PERIOD_OF_THE_CLAN_S_DISSOLUTION
                        );
                        return;
                     }

                     CHSiegeManager.getInstance().registerClan(clan, hall, player);
                  } else {
                     CHSiegeManager.getInstance().unRegisterClan(clan, hall);
                  }

                  player.sendPacket(new SiegeInfo(hall, player));
               }
            }
         }
      }
   }
}
