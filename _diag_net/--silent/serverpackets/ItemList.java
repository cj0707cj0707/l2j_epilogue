package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ItemList extends AbstractItemPacket {
   private final Player _player;
   private final Collection<Item> _items;
   private final boolean _showWindow;

   public ItemList(Player player, boolean showWindow) {
      this._player = player;
      this._showWindow = showWindow;
      this._items = player.getInventory().getItems();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ITEM_LIST.writeId(this, buffer);
      buffer.writeShort(this._showWindow);
      buffer.writeShort(this._items.size());

      for (Item item : this._items) {
         this.writeItem(item, buffer);
      }

      this.writeInventoryBlock(this._player.getInventory(), buffer);
   }
}
