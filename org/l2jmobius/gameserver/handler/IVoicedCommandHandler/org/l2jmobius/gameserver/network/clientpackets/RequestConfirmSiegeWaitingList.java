package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.SiegeDefenderList;

public class RequestConfirmSiegeWaitingList extends ClientPacket {
   private int _approved;
   private int _castleId;
   private int _clanId;

   @Override
   protected void readImpl() {
      this._castleId = this.readInt();
      this._clanId = this.readInt();
      this._approved = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.getClan() != null) {
            Castle castle = CastleManager.getInstance().getCastleById(this._castleId);
            if (castle != null) {
               if (castle.getOwnerId() == player.getClanId() && player.isClanLeader()) {
                  Clan clan = ClanTable.getInstance().getClan(this._clanId);
                  if (clan != null) {
                     if (!castle.getSiege().isRegistrationOver()) {
                        if (this._approved == 1) {
                           if (!castle.getSiege().checkIsDefenderWaiting(clan)) {
                              return;
                           }

                           castle.getSiege().approveSiegeDefenderClan(this._clanId);
                        } else if (castle.getSiege().checkIsDefenderWaiting(clan) || castle.getSiege().checkIsDefender(clan)) {
                           castle.getSiege().removeSiegeClan(this._clanId);
                        }
                     }

                     player.sendPacket(new SiegeDefenderList(castle));
                  }
               }
            }
         }
      }
   }
}
