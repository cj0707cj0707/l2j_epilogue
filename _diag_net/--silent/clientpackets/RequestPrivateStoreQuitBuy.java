package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;

public class RequestPrivateStoreQuitBuy extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.setPrivateStoreType(PrivateStoreType.NONE);
         player.standUp();
         player.broadcastUserInfo();
      }
   }
}
