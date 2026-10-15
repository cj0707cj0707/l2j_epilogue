package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestPrivateStoreManageBuy extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.isAlikeDead() && !player.isInOlympiadMode()) {
            player.tryOpenPrivateBuyStore();
         }
      }
   }
}
