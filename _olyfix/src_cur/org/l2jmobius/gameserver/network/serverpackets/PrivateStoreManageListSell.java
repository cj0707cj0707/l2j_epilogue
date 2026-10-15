package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreManageListSell extends ServerPacket {
   private final int _objId;
   private final long _playerAdena;
   private final boolean _packageSale;
   private final Collection<TradeItem> _itemList;
   private final Collection<TradeItem> _sellList;

   public PrivateStoreManageListSell(Player player, boolean isPackageSale) {
      this._objId = player.getObjectId();
      this._playerAdena = player.getAdena();
      player.getSellList().updateItems();
      this._packageSale = isPackageSale;
      this._itemList = player.getInventory().getAvailableItems(player.getSellList());
      this._sellList = player.getSellList().getItems();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PRIVATE_STORE_SELL_MANAGE_LIST.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._packageSale);
      buffer.writeLong(this._playerAdena);
      buffer.writeInt(this._itemList.size());

      for (TradeItem item : this._itemList) {
         buffer.writeInt(item.getItem().getType2());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItem().getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(0);
         buffer.writeShort(item.getEnchant());
         buffer.writeShort(item.getCustomType2());
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeLong(item.getPrice());
         buffer.writeShort((short)item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }

         for (int op : item.getEnchantOptions()) {
            buffer.writeShort(op);
         }
      }

      buffer.writeInt(this._sellList.size());

      for (TradeItem item : this._sellList) {
         buffer.writeInt(item.getItem().getType2());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItem().getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(0);
         buffer.writeShort(item.getEnchant());
         buffer.writeShort(0);
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeLong(item.getPrice());
         buffer.writeLong(item.getItem().getReferencePrice());
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
