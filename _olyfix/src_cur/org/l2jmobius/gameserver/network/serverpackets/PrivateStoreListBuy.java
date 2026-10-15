package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreListBuy extends ServerPacket {
   private final int _objId;
   private final long _playerAdena;
   private final Collection<TradeItem> _items;

   public PrivateStoreListBuy(Player player, Player storePlayer) {
      this._objId = storePlayer.getObjectId();
      this._playerAdena = player.getAdena();
      storePlayer.getSellList().updateItems();
      this._items = storePlayer.getBuyList().getAvailableItems(player.getInventory());
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PRIVATE_STORE_BUY_LIST.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeLong(this._playerAdena);
      buffer.writeInt(this._items.size());

      for (TradeItem item : this._items) {
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItem().getId());
         buffer.writeShort(item.getEnchant());
         buffer.writeLong(item.getCount());
         buffer.writeLong(item.getItem().getReferencePrice());
         buffer.writeShort(0);
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeShort(item.getItem().getType2());
         buffer.writeLong(item.getPrice());
         buffer.writeLong(item.getStoreCount());
         buffer.writeShort((short)item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }

         for (int op : item.getEnchantOptions()) {
            buffer.writeShort(op);
         }
      }
   }
}
