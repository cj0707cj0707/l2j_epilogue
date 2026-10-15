package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ExRpItemLink extends ServerPacket {
   private final Item _item;

   public ExRpItemLink(Item item) {
      this._item = item;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_RP_ITEM_LINK.writeId(this, buffer);
      buffer.writeInt(this._item.getObjectId());
      buffer.writeInt(this._item.getDisplayId());
      buffer.writeLong(this._item.getCount());
      buffer.writeShort(this._item.getTemplate().getType2());
      buffer.writeInt(this._item.getTemplate().getBodyPart().getMask());
      buffer.writeShort(this._item.getEnchantLevel());
      buffer.writeShort(this._item.getCustomType2());
      buffer.writeShort(0);
      if (this._item.isAugmented()) {
         buffer.writeInt(this._item.getAugmentation().getAugmentationId());
      } else {
         buffer.writeInt(0);
      }

      buffer.writeInt(this._item.getMana());
      buffer.writeShort(this._item.getAttackElementType());
      buffer.writeShort(this._item.getAttackElementPower());

      for (byte i = 0; i < 6; i++) {
         buffer.writeShort(this._item.getElementDefAttr(i));
      }

      for (int op : this._item.getEnchantOptions()) {
         buffer.writeShort(op);
      }
   }
}
