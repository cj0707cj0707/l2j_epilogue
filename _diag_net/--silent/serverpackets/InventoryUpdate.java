package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class InventoryUpdate extends AbstractInventoryUpdate {
   public InventoryUpdate() {
   }

   public InventoryUpdate(Item item) {
      super(item);
   }

   public InventoryUpdate(List<ItemInfo> items) {
      super(items);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.INVENTORY_UPDATE.writeId(this, buffer);
      this.writeItems(buffer);
   }
}
