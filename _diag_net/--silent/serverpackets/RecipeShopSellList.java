package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.recipe.ManufactureItem;

public class RecipeShopSellList extends ServerPacket {
   private final Player _buyer;
   private final Player _manufacturer;

   public RecipeShopSellList(Player buyer, Player manufacturer) {
      this._buyer = buyer;
      this._manufacturer = manufacturer;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.RECIPE_SHOP_SELL_LIST.writeId(this, buffer);
      buffer.writeInt(this._manufacturer.getObjectId());
      buffer.writeInt((int)this._manufacturer.getCurrentMp());
      buffer.writeInt(this._manufacturer.getMaxMp());
      buffer.writeLong(this._buyer.getAdena());
      if (!this._manufacturer.hasManufactureShop()) {
         buffer.writeInt(0);
      } else {
         buffer.writeInt(this._manufacturer.getManufactureItems().size());

         for (ManufactureItem temp : this._manufacturer.getManufactureItems().values()) {
            buffer.writeInt(temp.getRecipeId());
            buffer.writeInt(0);
            buffer.writeLong(temp.getCost());
         }
      }
   }
}
