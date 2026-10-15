package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;

public class RequestRecipeShopManageList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isAlikeDead()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            if (player.isInStoreMode()) {
               player.setPrivateStoreType(PrivateStoreType.NONE);
               player.broadcastUserInfo();
               if (player.isSitting()) {
                  player.standUp();
               }
            }

            player.sendPacket(new RecipeShopManageList(player, true));
         }
      }
   }
}
