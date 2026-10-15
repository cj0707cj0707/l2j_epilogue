package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBrGamePoint extends ServerPacket {
   private final int _playerObj;
   private long _points;

   public ExBrGamePoint(Player player) {
      this._playerObj = player.getObjectId();
      if (GeneralConfig.PRIME_SHOP_ITEM_ID == -1) {
         this._points = player.getGamePoints();
      } else {
         this._points = player.getInventory().getInventoryItemCount(GeneralConfig.PRIME_SHOP_ITEM_ID, -1);
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BR_GAME_POINT.writeId(this, buffer);
      buffer.writeInt(this._playerObj);
      buffer.writeLong(this._points);
      buffer.writeInt(0);
   }
}
