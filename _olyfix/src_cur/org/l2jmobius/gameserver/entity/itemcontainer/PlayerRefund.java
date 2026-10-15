package org.l2jmobius.gameserver.entity.itemcontainer;

import java.util.logging.Level;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;

public class PlayerRefund extends ItemContainer {
   private final Player _owner;

   public PlayerRefund(Player owner) {
      this._owner = owner;
   }

   @Override
   public String getName() {
      return "Refund";
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   public ItemLocation getBaseLocation() {
      return ItemLocation.REFUND;
   }

   @Override
   protected void addItem(Item item) {
      super.addItem(item);

      try {
         if (this.getSize() > 12) {
            Item removedItem = this._items.stream().findFirst().get();
            if (this._items.remove(removedItem)) {
               ItemManager.destroyItem(ItemProcessType.REFUND, removedItem, this.getOwner(), null);
               removedItem.updateDatabase(true);
            }
         }
      } catch (Exception var3) {
         LOGGER.log(Level.SEVERE, "addItem()", (Throwable)var3);
      }
   }

   @Override
   public void refreshWeight() {
   }

   @Override
   public void deleteMe() {
      try {
         for (Item item : this._items) {
            if (item != null) {
               ItemManager.destroyItem(ItemProcessType.REFUND, item, this.getOwner(), null);
               item.updateDatabase(true);
            }
         }
      } catch (Exception var3) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var3);
      }

      this._items.clear();
   }

   @Override
   public void restore() {
   }
}
