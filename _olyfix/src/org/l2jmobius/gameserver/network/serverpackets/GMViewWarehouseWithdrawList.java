package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GMViewWarehouseWithdrawList extends AbstractItemPacket {
   private final Collection<Item> _items;
   private final String _playerName;
   private final long _money;

   public GMViewWarehouseWithdrawList(Player player) {
      this._items = player.getWarehouse().getItems();
      this._playerName = player.getName();
      this._money = player.getWarehouse().getAdena();
   }

   public GMViewWarehouseWithdrawList(Clan clan) {
      this._playerName = clan.getLeaderName();
      this._items = clan.getWarehouse().getItems();
      this._money = clan.getWarehouse().getAdena();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.GM_VIEW_WAREHOUSE_WITHDRAW_LIST.writeId(this, buffer);
      buffer.writeString(this._playerName);
      buffer.writeLong(this._money);
      buffer.writeShort(this._items.size());

      for (Item item : this._items) {
         this.writeItem(item, buffer);
         buffer.writeInt(item.getObjectId());
      }
   }
}
