package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

public class RecipeItemMakeInfo extends ServerPacket {
   private final int _id;
   private final Player _player;
   private final boolean _success;

   public RecipeItemMakeInfo(int id, Player player, boolean success) {
      this._id = id;
      this._player = player;
      this._success = success;
   }

   public RecipeItemMakeInfo(int id, Player player) {
      this._id = id;
      this._player = player;
      this._success = true;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      RecipeList recipe = RecipeData.getInstance().getRecipeList(this._id);
      if (recipe == null) {
         PacketLogger.info("Character: " + this._player + ": Requested unexisting recipe with id = " + this._id);
      } else {
         ServerPackets.RECIPE_ITEM_MAKE_INFO.writeId(this, buffer);
         buffer.writeInt(this._id);
         buffer.writeInt(!recipe.isDwarvenRecipe());
         buffer.writeInt((int)this._player.getCurrentMp());
         buffer.writeInt(this._player.getMaxMp());
         buffer.writeInt(this._success);
      }
   }
}
