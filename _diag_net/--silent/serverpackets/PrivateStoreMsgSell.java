package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PrivateStoreMsgSell extends ServerPacket {
   private final int _objectId;
   private String _message;

   public PrivateStoreMsgSell(Player player) {
      this._objectId = player.getObjectId();
      if (player.getSellList() != null || player.isSellingBuffs()) {
         this._message = player.getSellList().getTitle();
      }
   }

   public PrivateStoreMsgSell(int objectId, String message) {
      this._objectId = objectId;
      this._message = message;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PRIVATE_STORE_SELL_MSG.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeString(this._message);
   }
}
