package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopItemInfo;

public class RequestRecipeShopMakeInfo extends ClientPacket {
   private int _playerObjectId;
   private int _recipeId;

   @Override
   protected void readImpl() {
      this._playerObjectId = this.readInt();
      this._recipeId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player shop = World.getPlayer(this._playerObjectId);
         if (shop != null && shop.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
            player.sendPacket(new RecipeShopItemInfo(shop, this._recipeId));
         }
      }
   }
}
