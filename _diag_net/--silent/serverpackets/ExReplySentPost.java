package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.network.holders.MailMessage;

public class ExReplySentPost extends ServerPacket {
   private final MailMessage _msg;
   private Collection<Item> _items = null;

   public ExReplySentPost(MailMessage msg) {
      this._msg = msg;
      if (msg.hasAttachments()) {
         ItemContainer attachments = msg.getAttachments();
         if (attachments != null && attachments.getSize() > 0) {
            this._items = attachments.getItems();
         } else {
            org.l2jmobius.gameserver.network.PacketLogger.warning("Message " + msg.getId() + " has attachments but itemcontainer is empty.");
         }
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_REPLY_SENT_POST.writeId(this, buffer);
      buffer.writeInt(this._msg.getId());
      buffer.writeInt(this._msg.isLocked());
      buffer.writeString(this._msg.getReceiverName());
      buffer.writeString(this._msg.getSubject());
      buffer.writeString(this._msg.getContent());
      if (this._items != null && !this._items.isEmpty()) {
         buffer.writeInt(this._items.size());

         for (Item item : this._items) {
            buffer.writeShort(item.getTemplate().getType2());
            buffer.writeInt(0);
            buffer.writeInt(item.getId());
            buffer.writeLong(item.getCount());
            buffer.writeInt(item.getEnchantLevel());
            buffer.writeShort(item.getCustomType2());
            buffer.writeShort(0);
            buffer.writeInt(0);
            buffer.writeInt(item.isAugmented() ? item.getAugmentation().getAugmentationId() : 0);
            buffer.writeInt(0);
            buffer.writeShort(item.getAttackElementType());
            buffer.writeShort(item.getAttackElementPower());

            for (byte i = 0; i < 6; i++) {
               buffer.writeShort(item.getElementDefAttr(i));
            }

            for (int op : item.getEnchantOptions()) {
               buffer.writeShort(op);
            }
         }

         buffer.writeLong(this._msg.getReqAdena());
         buffer.writeInt(this._msg.getSendBySystem());
      } else {
         buffer.writeInt(0);
         buffer.writeLong(this._msg.getReqAdena());
      }
   }
}
