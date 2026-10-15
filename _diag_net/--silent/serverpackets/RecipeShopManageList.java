package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.Iterator;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;

public class RecipeShopManageList extends ServerPacket {
   private final Player _seller;
   private final boolean _isDwarven;
   private Collection<RecipeList> _recipes;

   public RecipeShopManageList(Player seller, boolean isDwarven) {
      this._seller = seller;
      this._isDwarven = isDwarven;
      if (this._isDwarven && this._seller.hasDwarvenCraft()) {
         this._recipes = this._seller.getDwarvenRecipeBook();
      } else {
         this._recipes = this._seller.getCommonRecipeBook();
      }

      if (this._seller.hasManufactureShop()) {
         Iterator<ManufactureItem> it = this._seller.getManufactureItems().values().iterator();

         while (it.hasNext()) {
            ManufactureItem item = it.next();
            if (item.isDwarven() != this._isDwarven || !seller.hasRecipeList(item.getRecipeId())) {
               it.remove();
            }
         }
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.RECIPE_SHOP_MANAGE_LIST.writeId(this, buffer);
      buffer.writeInt(this._seller.getObjectId());
      buffer.writeInt((int)this._seller.getAdena());
      buffer.writeInt(!this._isDwarven);
      if (this._recipes == null) {
         buffer.writeInt(0);
      } else {
         buffer.writeInt(this._recipes.size());
         int count = 0;

         for (RecipeList recipe : this._recipes) {
            count++;
            buffer.writeInt(recipe.getId());
            buffer.writeInt(count);
         }
      }

      if (!this._seller.hasManufactureShop()) {
         buffer.writeInt(0);
      } else {
         buffer.writeInt(this._seller.getManufactureItems().size());

         for (ManufactureItem item : this._seller.getManufactureItems().values()) {
            buffer.writeInt(item.getRecipeId());
            buffer.writeInt(0);
            buffer.writeLong(item.getCost());
         }
      }
   }
}
