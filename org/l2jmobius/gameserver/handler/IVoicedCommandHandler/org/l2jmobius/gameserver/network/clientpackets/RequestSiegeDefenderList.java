package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.SiegeDefenderList;

public class RequestSiegeDefenderList extends ClientPacket {
   private int _castleId;

   @Override
   protected void readImpl() {
      this._castleId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Castle castle = CastleManager.getInstance().getCastleById(this._castleId);
      if (castle != null) {
         Player player = this.getPlayer();
         if (player != null) {
            player.sendPacket(new SiegeDefenderList(castle));
         }
      }
   }
}
