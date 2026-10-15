package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.managers.RecipeManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class RequestRecipeShopMakeItem extends ClientPacket {
   private int _id;
   private int _recipeId;
   private long _unknown;

   @Override
   protected void readImpl() {
      this._id = this.readInt();
      this._recipeId = this.readInt();
      this._unknown = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canManufacture()) {
            Player manufacturer = World.getPlayer(this._id);
            if (manufacturer != null) {
               if (manufacturer.getInstanceId() == player.getInstanceId() || player.getInstanceId() == -1) {
                  if (player.isInStoreMode()) {
                     player.sendMessage("You cannot create items while trading.");
                  } else if (manufacturer.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
                     if (!player.isCrafting() && !manufacturer.isCrafting()) {
                        if (LocationUtil.checkIfInRange(150, player, manufacturer, true)) {
                           RecipeManager.getInstance().requestManufactureItem(manufacturer, this._recipeId, player);
                        }
                     } else {
                        player.sendMessage("You are currently in Craft Mode.");
                     }
                  }
               }
            }
         }
      }
   }
}
