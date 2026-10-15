package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.multisell.Entry;
import org.l2jmobius.gameserver.mechanics.multisell.Ingredient;
import org.l2jmobius.gameserver.mechanics.multisell.ListContainer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MultiSellList extends ServerPacket {
   private int _size;
   private int _index;
   private final ListContainer _list;
   private final boolean _finished;

   public MultiSellList(ListContainer list, int index) {
      this._list = list;
      this._index = index;
      this._size = list.getEntries().size() - index;
      if (this._size > 40) {
         this._finished = false;
         this._size = 40;
      } else {
         this._finished = true;
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MULTI_SELL_LIST.writeId(this, buffer);
      buffer.writeInt(this._list.getListId());
      buffer.writeInt(1 + this._index / 40);
      buffer.writeInt(this._finished);
      buffer.writeInt(40);
      buffer.writeInt(this._size);

      while (this._size-- > 0) {
         Entry ent = this._list.getEntries().get(this._index++);
         buffer.writeInt(ent.getEntryId());
         buffer.writeByte(ent.isStackable());
         buffer.writeShort(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeShort(65534);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(ent.getProducts().size());
         buffer.writeShort(ent.getIngredients().size());

         for (Ingredient ing : ent.getProducts()) {
            if (ing.getTemplate() != null) {
               buffer.writeInt(ing.getTemplate().getDisplayId());
               buffer.writeInt(ing.getTemplate().getBodyPart().getMask());
               buffer.writeShort(ing.getTemplate().getType2());
            } else {
               buffer.writeInt(ing.getItemId());
               buffer.writeInt(0);
               buffer.writeShort(65535);
            }

            buffer.writeLong(ing.getItemCount());
            if (ing.getItemInfo() != null) {
               buffer.writeShort(ing.getItemInfo().getEnchantLevel());
               buffer.writeInt(ing.getItemInfo().getAugmentId());
               buffer.writeInt(0);
               buffer.writeShort((short)ing.getItemInfo().getElementId());
               buffer.writeShort(ing.getItemInfo().getElementPower());
               buffer.writeShort(ing.getItemInfo().getElementals()[0]);
               buffer.writeShort(ing.getItemInfo().getElementals()[1]);
               buffer.writeShort(ing.getItemInfo().getElementals()[2]);
               buffer.writeShort(ing.getItemInfo().getElementals()[3]);
               buffer.writeShort(ing.getItemInfo().getElementals()[4]);
               buffer.writeShort(ing.getItemInfo().getElementals()[5]);
            } else {
               buffer.writeShort(ing.getEnchantLevel());
               buffer.writeInt(0);
               buffer.writeInt(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
            }
         }

         for (Ingredient ing : ent.getIngredients()) {
            buffer.writeInt(ing.getTemplate() != null ? ing.getTemplate().getDisplayId() : ing.getItemId());
            buffer.writeShort(ing.getTemplate() != null ? ing.getTemplate().getType2() : '\uffff');
            buffer.writeLong(ing.getItemCount());
            if (ing.getItemInfo() != null) {
               buffer.writeShort(ing.getItemInfo().getEnchantLevel());
               buffer.writeInt(ing.getItemInfo().getAugmentId());
               buffer.writeInt(0);
               buffer.writeShort((short)ing.getItemInfo().getElementId());
               buffer.writeShort(ing.getItemInfo().getElementPower());
               buffer.writeShort(ing.getItemInfo().getElementals()[0]);
               buffer.writeShort(ing.getItemInfo().getElementals()[1]);
               buffer.writeShort(ing.getItemInfo().getElementals()[2]);
               buffer.writeShort(ing.getItemInfo().getElementals()[3]);
               buffer.writeShort(ing.getItemInfo().getElementals()[4]);
               buffer.writeShort(ing.getItemInfo().getElementals()[5]);
            } else {
               buffer.writeShort(ing.getEnchantLevel());
               buffer.writeInt(0);
               buffer.writeInt(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
            }
         }
      }
   }
}
