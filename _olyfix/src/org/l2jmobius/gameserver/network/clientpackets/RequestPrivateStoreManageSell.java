package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestPrivateStoreManageSell extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isAlikeDead() || player.isInOlympiadMode()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }
}
