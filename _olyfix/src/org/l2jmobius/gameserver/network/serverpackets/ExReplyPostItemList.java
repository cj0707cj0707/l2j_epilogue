package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExReplyPostItemList extends ServerPacket {
   private final Player _player;
   private final Collection<Item> _itemList;

   public ExReplyPostItemList(Player player) {
      this._player = player;
      this._itemList = this._player.getInventory().getAvailableItems(true, false, false);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_REPLY_POST_ITEM_LIST.writeId(this, buffer);
      buffer.writeInt(this._itemList.size());

      for (Item item : this._itemList) {
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(item.getTemplate().getType2());
         buffer.writeShort(item.getCustomType1());
         buffer.writeInt(item.getTemplate().getBodyPart().getMask());
         buffer.writeShort(item.getEnchantLevel());
         buffer.writeShort(item.getCustomType2());
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
