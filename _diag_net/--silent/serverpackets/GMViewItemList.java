package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class GMViewItemList extends AbstractItemPacket {
   private final List<Item> _items = new ArrayList<>();
   private final int _limit;
   private final String _playerName;

   public GMViewItemList(Player player) {
      this._playerName = player.getName();
      this._limit = player.getInventoryLimit();

      for (Item item : player.getInventory().getItems()) {
         this._items.add(item);
      }
   }

   public GMViewItemList(Pet cha) {
      this._playerName = cha.getName();
      this._limit = cha.getInventoryLimit();

      for (Item item : cha.getInventory().getItems()) {
         this._items.add(item);
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GM_VIEW_ITEM_LIST.writeId(this, buffer);
      buffer.writeString(this._playerName);
      buffer.writeInt(this._limit);
      buffer.writeShort(1);
      buffer.writeShort(this._items.size());

      for (Item item : this._items) {
         this.writeItem(item, buffer);
      }
   }
}
