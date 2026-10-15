package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;

public class RequestSaveInventoryOrder extends ClientPacket {
   private List<RequestSaveInventoryOrder.InventoryOrder> _order;
   private static final int LIMIT = 125;

   @Override
   protected void readImpl() {
      int sz = this.readInt();
      sz = Math.min(sz, 125);
      this._order = new ArrayList<>(sz);

      for (int i = 0; i < sz; i++) {
         int objectId = this.readInt();
         int order = this.readInt();
         this._order.add(new RequestSaveInventoryOrder.InventoryOrder(objectId, order));
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Inventory inventory = player.getInventory();

         for (RequestSaveInventoryOrder.InventoryOrder order : this._order) {
            Item item = inventory.getItemByObjectId(order.objectID);
            if (item != null && item.getItemLocation() == ItemLocation.INVENTORY) {
               item.setItemLocation(ItemLocation.INVENTORY, order.order, false);
            }
         }
      }
   }

   private static class InventoryOrder {
      int order;
      int objectID;

      public InventoryOrder(int id, int ord) {
         this.objectID = id;
         this.order = ord;
      }
   }
}
