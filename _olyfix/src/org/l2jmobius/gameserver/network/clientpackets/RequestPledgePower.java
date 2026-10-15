package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.network.serverpackets.ManagePledgePower;

public class RequestPledgePower extends ClientPacket {
   private int _rank;
   private int _action;
   private int _privs;

   @Override
   protected void readImpl() {
      this._rank = this.readInt();
      this._action = this.readInt();
      if (this._action == 2) {
         this._privs = this.readInt();
      } else {
         this._privs = 0;
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._action == 2) {
            if (player.isClanLeader()) {
               if (this._rank == 9) {
                  this._privs = this._privs
                     & (ClanAccess.ACCESS_WAREHOUSE.getMask() | ClanAccess.HALL_OPEN_DOOR.getMask() | ClanAccess.CASTLE_OPEN_DOOR.getMask());
               }

               player.getClan().setRankPrivs(this._rank, this._privs);
            }
         } else {
            player.sendPacket(new ManagePledgePower(player.getClan(), this._action, this._rank));
         }
      }
   }
}
