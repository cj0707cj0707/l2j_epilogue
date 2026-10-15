package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;

public class RequestRecipeShopManagePrev extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.isAlikeDead() && player.getTarget() != null && player.getTarget().isPlayer()) {
            player.sendPacket(new RecipeShopSellList(player, player.getTarget().asPlayer()));
         } else {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }
}
