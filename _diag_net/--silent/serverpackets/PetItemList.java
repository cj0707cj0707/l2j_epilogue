package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class PetItemList extends ServerPacket {
   private final Collection<Item> _items;

   public PetItemList(Collection<Item> collection) {
      this._items = collection;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PET_ITEM_LIST.writeId(this, buffer);
      buffer.writeShort(this._items.size());

      for (Item item : this._items) {
         buffer.writeShort(item.getTemplate().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(item.getTemplate().getType2());
         buffer.writeShort(0);
         buffer.writeShort(item.isEquipped());
         buffer.writeInt(item.getTemplate().getBodyPart().getMask());
         buffer.writeShort(item.getEnchantLevel());
         buffer.writeShort(0);
         buffer.writeShort(item.getAttackElementType());
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
