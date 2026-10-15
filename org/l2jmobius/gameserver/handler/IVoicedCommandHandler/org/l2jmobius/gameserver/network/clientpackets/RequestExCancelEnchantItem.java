package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;

public class RequestExCancelEnchantItem extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new EnchantResult(2, 0, 0));
         player.setActiveEnchantItemId(-1);
      }
   }
}
