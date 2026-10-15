package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;

public class TradeUpdate extends ServerPacket {
   private final Collection<Item> _items;
   private final Collection<TradeItem> _tradeItems;

   public TradeUpdate(TradeList trade, Player player) {
      this._items = player.getInventory().getItems();
      this._tradeItems = trade.getItems();
   }

   private long getItemCount(int objectId) {
      for (Item item : this._items) {
         if (item.getObjectId() == objectId) {
            return item.getCount();
         }
      }

      return 0L;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TRADE_UPDATE.writeId(this, buffer);
      buffer.writeShort(this._tradeItems.size());

      for (TradeItem item : this._tradeItems) {
         long aveCount = this.getItemCount(item.getObjectId()) - item.getCount();
         boolean stackable = item.getItem().isStackable();
         if (aveCount == 0L) {
            aveCount = 1L;
            stackable = false;
         }

         buffer.writeShort(stackable ? 3 : 2);
         buffer.writeShort(item.getItem().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItem().getId());
         buffer.writeLong(aveCount);
         buffer.writeShort(item.getItem().getType2());
         buffer.writeShort(0);
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeShort(item.getEnchant());
         buffer.writeShort(0);
         buffer.writeShort(item.getCustomType2());
         buffer.writeShort((short)item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }
      }
   }
}
