package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

public class WareHouseWithdrawalList extends ServerPacket {
   public static final int PRIVATE = 1;
   public static final int CLAN = 4;
   public static final int CASTLE = 3;
   public static final int FREIGHT = 1;
   private long _playerAdena;
   private Collection<Item> _items;
   private int _whType;

   public WareHouseWithdrawalList(Player player, int type) {
      if (player.getActiveWarehouse() == null) {
         PacketLogger.warning("Error while sending withdraw request to: " + player.getName());
      } else {
         this._playerAdena = player.getAdena();
         this._items = player.getActiveWarehouse().getItems();
         this._whType = type;
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.WAREHOUSE_WITHDRAW_LIST.writeId(this, buffer);
      buffer.writeShort(this._whType);
      buffer.writeLong(this._playerAdena);
      buffer.writeShort(this._items.size());

      for (Item item : this._items) {
         buffer.writeShort(item.getTemplate().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(item.getTemplate().getType2());
         buffer.writeShort(item.getCustomType1());
         buffer.writeInt(item.getTemplate().getBodyPart().getMask());
         buffer.writeShort(item.getEnchantLevel());
         buffer.writeShort(0);
         buffer.writeShort(item.getCustomType2());
         buffer.writeInt(item.getObjectId());
         if (item.isAugmented()) {
            buffer.writeInt(65535 & item.getAugmentation().getAugmentationId());
            buffer.writeInt(item.getAugmentation().getAugmentationId() >> 16);
         } else {
            buffer.writeLong(0L);
         }

         buffer.writeShort((short)item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }

         buffer.writeInt(item.getMana());
         buffer.writeInt(item.isTimeLimitedItem() ? (int)(item.getRemainingTime() / 1000L) : -1);

         for (int op : item.getEnchantOptions()) {
            buffer.writeShort(op);
         }
      }
   }
}
