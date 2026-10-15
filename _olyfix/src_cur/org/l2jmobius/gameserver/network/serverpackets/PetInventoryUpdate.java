package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PetInventoryUpdate extends AbstractInventoryUpdate {
   public PetInventoryUpdate() {
   }

   public PetInventoryUpdate(Item item) {
      super(item);
   }

   public PetInventoryUpdate(List<ItemInfo> items) {
      super(items);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PET_INVENTORY_UPDATE.writeId(this, buffer);
      buffer.writeShort(this.getItems().size());

      for (ItemInfo item : this.getItems()) {
         buffer.writeShort(item.getChange());
         buffer.writeShort(item.getItem().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItem().getId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(item.getItem().getType2());
         buffer.writeShort(0);
         buffer.writeShort(item.getEquipped());
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeShort(item.getEnchant());
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
