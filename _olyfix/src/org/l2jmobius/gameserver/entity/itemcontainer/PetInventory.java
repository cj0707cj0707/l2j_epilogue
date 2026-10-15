package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class PetInventory extends Inventory {
   private final Pet _owner;

   public PetInventory(Pet owner) {
      this._owner = owner;
   }

   public Pet getOwner() {
      return this._owner;
   }

   @Override
   public int getOwnerId() {
      try {
         return this._owner.getOwner().getObjectId();
      } catch (NullPointerException var3) {
         return 0;
      }
   }

   @Override
   protected void refreshWeight() {
      super.refreshWeight();
      this._owner.updateAndBroadcastStatus(1);
   }

   public boolean validateCapacity(Item item) {
      int slots = 0;
      if ((!item.isStackable() || this.getItemByItemId(item.getId()) == null) && !item.getTemplate().hasExImmediateEffect()) {
         slots++;
      }

      return this.validateCapacity(slots);
   }

   @Override
   public boolean validateCapacity(long slots) {
      return this._items.size() + slots <= this._owner.getInventoryLimit();
   }

   public boolean validateWeight(Item item, long count) {
      int weight = 0;
      ItemTemplate template = ItemData.getInstance().getTemplate(item.getId());
      if (template == null) {
         return false;
      } else {
         weight = (int)(weight + count * template.getWeight());
         return this.validateWeight(weight);
      }
   }

   @Override
   public boolean validateWeight(long weight) {
      return this._totalWeight + weight <= this._owner.getMaxLoad();
   }

   @Override
   protected ItemLocation getBaseLocation() {
      return ItemLocation.PET;
   }

   @Override
   protected ItemLocation getEquipLocation() {
      return ItemLocation.PET_EQUIP;
   }

   @Override
   public void restore() {
      super.restore();

      for (Item item : this._items) {
         if (item.isEquipped() && !item.getTemplate().checkCondition(this._owner, this._owner, false)) {
            this.unEquipItemInSlot(item.getLocationSlot());
         }
      }
   }

   public void transferItemsToOwner() {
      for (Item item : this._items) {
         this.getOwner()
            .transferItem(
               ItemProcessType.TRANSFER,
               item.getObjectId(),
               item.getCount(),
               this.getOwner().getOwner().getInventory(),
               this.getOwner().getOwner(),
               this.getOwner()
            );
      }
   }
}
