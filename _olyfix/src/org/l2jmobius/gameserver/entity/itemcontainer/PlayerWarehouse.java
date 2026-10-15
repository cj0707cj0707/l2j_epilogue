package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;

public class PlayerWarehouse extends Warehouse {
   private final Player _owner;

   public PlayerWarehouse(Player owner) {
      this._owner = owner;
   }

   @Override
   public String getName() {
      return "Warehouse";
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   public ItemLocation getBaseLocation() {
      return ItemLocation.WAREHOUSE;
   }

   @Override
   public boolean validateCapacity(long slots) {
      return this._items.size() + slots <= this._owner.getWareHouseLimit();
   }
}
