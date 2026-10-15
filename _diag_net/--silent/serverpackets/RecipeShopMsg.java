package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class RecipeShopMsg extends ServerPacket {
   private final int _objectId;
   private final String _message;

   public RecipeShopMsg(Player player) {
      this._objectId = player.getObjectId();
      this._message = player.getStoreName();
   }

   public RecipeShopMsg(int objectId, String message) {
      this._objectId = objectId;
      this._message = message;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.RECIPE_SHOP_MSG.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeString(this._message);
   }
}
