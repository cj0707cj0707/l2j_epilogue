package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

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
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.INVENTORY_UPDATE.writeId(this, buffer);
      this.writeItems(buffer);
   }
}
