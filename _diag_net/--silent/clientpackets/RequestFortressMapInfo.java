package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowFortressMapInfo;

public class RequestFortressMapInfo extends ClientPacket {
   private int _fortressId;

   @Override
   protected void readImpl() {
      this._fortressId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Fort fort = FortManager.getInstance().getFortById(this._fortressId);
         if (fort == null) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "Fort is not found with id ("
                  + this._fortressId
                  + ") in all forts with size of ("
                  + FortManager.getInstance().getForts().size()
                  + ") called by player ("
                  + player
                  + ")"
            );
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            player.sendPacket(new ExShowFortressMapInfo(fort));
         }
      }
   }
}
