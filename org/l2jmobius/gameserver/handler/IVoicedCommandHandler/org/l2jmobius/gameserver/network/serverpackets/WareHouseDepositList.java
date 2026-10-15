package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class WareHouseDepositList extends ServerPacket {
   public static final int PRIVATE = 1;
   public static final int CLAN = 4;
   public static final int CASTLE = 3;
   public static final int FREIGHT = 1;
   private final long _playerAdena;
   private final List<Item> _items = new ArrayList<>();
   private final int _whType;

   public WareHouseDepositList(Player player, int type) {
      this._whType = type;
      this._playerAdena = player.getAdena();
      boolean isPrivate = this._whType == 1;

      for (Item temp : player.getInventory().getAvailableItems(true, isPrivate, false)) {
         if (temp != null && temp.isDepositable(isPrivate)) {
            this._items.add(temp);
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.WAREHOUSE_DEPOSIT_LIST.writeId(this, buffer);
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
