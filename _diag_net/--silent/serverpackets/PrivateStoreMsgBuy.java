package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PrivateStoreMsgBuy extends ServerPacket {
   private final int _objectId;
   private String _message;

   public PrivateStoreMsgBuy(Player player) {
      this._objectId = player.getObjectId();
      if (player.getBuyList() != null) {
         this._message = player.getBuyList().getTitle();
      }
   }

   public PrivateStoreMsgBuy(int objectId, String message) {
      this._objectId = objectId;
      this._message = message;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PRIVATE_STORE_BUY_MSG.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeString(this._message);
   }
}
