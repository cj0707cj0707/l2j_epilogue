package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class RecipeShopItemInfo extends ServerPacket {
   private final Player _player;
   private final int _recipeId;

   public RecipeShopItemInfo(Player player, int recipeId) {
      this._player = player;
      this._recipeId = recipeId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.RECIPE_SHOP_ITEM_INFO.writeId(this, buffer);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._recipeId);
      buffer.writeInt((int)this._player.getCurrentMp());
      buffer.writeInt(this._player.getMaxMp());
      buffer.writeInt(-1);
   }
}
