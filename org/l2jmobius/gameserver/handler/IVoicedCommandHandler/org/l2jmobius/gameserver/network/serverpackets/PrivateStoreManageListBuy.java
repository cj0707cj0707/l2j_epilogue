package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreManageListBuy extends ServerPacket {
   private final int _objId;
   private final long _playerAdena;
   private final Collection<Item> _itemList;
   private final Collection<TradeItem> _buyList;

   public PrivateStoreManageListBuy(Player player) {
      this._objId = player.getObjectId();
      this._playerAdena = player.getAdena();
      this._itemList = player.getInventory().getUniqueItems(false, true, true, false);
      this._buyList = player.getBuyList().getItems();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PRIVATE_STORE_BUY_MANAGE_LIST.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeLong(this._playerAdena);
      buffer.writeInt(this._itemList.size());

      for (Item item : this._itemList) {
         buffer.writeInt(item.getId());
         buffer.writeShort(0);
         buffer.writeLong(item.getCount());
         buffer.writeLong(item.getReferencePrice());
         buffer.writeShort(0);
         buffer.writeInt(item.getTemplate().getBodyPart().getMask());
         buffer.writeShort(item.getTemplate().getType2());
         buffer.writeShort((short)item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }

         for (int op : item.getEnchantOptions()) {
            buffer.writeShort(op);
         }
      }

      buffer.writeInt(this._buyList.size());

      for (TradeItem item : this._buyList) {
         buffer.writeInt(item.getItem().getId());
         buffer.writeShort(0);
         buffer.writeLong(item.getCount());
         buffer.writeLong(item.getItem().getReferencePrice());
         buffer.writeShort(0);
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeShort(item.getItem().getType2());
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
