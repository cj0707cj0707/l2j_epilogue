package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public abstract class AbstractItemPacket extends ServerPacket {
   protected void writeItem(TradeItem item, WriteBuffer buffer) {
      this.writeItem(new ItemInfo(item), buffer);
   }

   protected void writeItem(Item item, WriteBuffer buffer) {
      this.writeItem(new ItemInfo(item), buffer);
   }

   protected void writeItem(ItemInfo item, WriteBuffer buffer) {
      buffer.writeShort(item.getItem().getType1());
      buffer.writeInt(item.getObjectId());
      buffer.writeInt(item.getItem().getDisplayId());
      buffer.writeInt(item.getLocation());
      buffer.writeLong(item.getCount());
      buffer.writeShort(item.getItem().getType2());
      buffer.writeShort(item.getCustomType1());
      buffer.writeShort(item.getEquipped());
      buffer.writeInt(item.getItem().getBodyPart().getMask());
      buffer.writeShort(item.getEnchant());
      buffer.writeShort(item.getCustomType2());
      buffer.writeInt(item.getAugmentationBonus());
      buffer.writeInt(item.getMana());
      buffer.writeShort(item.getAttackElementType());
      buffer.writeShort(item.getAttackElementPower());

      for (byte i = 0; i < 6; i++) {
         buffer.writeShort(item.getElementDefAttr(i));
      }

      buffer.writeInt(item.getTime());

      for (int op : item.getEnchantOptions()) {
         buffer.writeShort(op);
      }
   }

   protected void writeInventoryBlock(PlayerInventory inventory, WriteBuffer buffer) {
      if (inventory.hasInventoryBlock()) {
         buffer.writeShort(inventory.getBlockItems().length);
         buffer.writeByte(inventory.getBlockMode());

         for (int i : inventory.getBlockItems()) {
            buffer.writeInt(i);
         }
      } else {
         buffer.writeShort(0);
      }
   }
}
