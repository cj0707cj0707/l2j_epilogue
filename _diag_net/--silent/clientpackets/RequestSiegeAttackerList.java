package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.serverpackets.SiegeAttackerList;

public class RequestSiegeAttackerList extends ClientPacket {
   private int _castleId;

   @Override
   protected void readImpl() {
      this._castleId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Castle castle = CastleManager.getInstance().getCastleById(this._castleId);
         if (castle != null) {
            player.sendPacket(new SiegeAttackerList(castle));
         } else {
            SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(this._castleId);
            if (hall != null) {
               player.sendPacket(new SiegeAttackerList(hall));
            }
         }
      }
   }
}
