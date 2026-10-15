package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PackageSendableList extends AbstractItemPacket {
   private final Collection<Item> _items;
   private final int _playerObjId;
   private final long _adena;

   public PackageSendableList(Collection<Item> items, int playerObjId, long adena) {
      this._items = items;
      this._playerObjId = playerObjId;
      this._adena = adena;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PACKAGE_SENDABLE_LIST.writeId(this, buffer);
      buffer.writeInt(this._playerObjId);
      buffer.writeLong(this._adena);
      buffer.writeInt(this._items.size());

      for (Item item : this._items) {
         this.writeItem(item, buffer);
         buffer.writeInt(item.getObjectId());
      }
   }
}
