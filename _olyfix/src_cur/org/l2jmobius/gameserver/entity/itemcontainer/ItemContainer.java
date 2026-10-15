package org.l2jmobius.gameserver.entity.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

public abstract class ItemContainer {
   protected static final Logger LOGGER = Logger.getLogger(ItemContainer.class.getName());
   protected final Set<Item> _items = ConcurrentHashMap.newKeySet(1);

   protected ItemContainer() {
   }

   protected abstract Creature getOwner();

   protected abstract ItemLocation getBaseLocation();

   public String getName() {
      return "ItemContainer";
   }

   public int getOwnerId() {
      return this.getOwner() == null ? 0 : this.getOwner().getObjectId();
   }

   public int getSize() {
      return this._items.size();
   }

   public Collection<Item> getItems() {
      return this._items;
   }

   public Item getItemByItemId(int itemId) {
      for (Item item : this._items) {
         if (item.getId() == itemId) {
            return item;
         }
      }

      return null;
   }

   public boolean haveItemForSelfResurrection() {
      for (Item item : this._items) {
         if (item != null && item.getTemplate().isAllowSelfResurrection()) {
            return true;
         }
      }

      return false;
   }

   public Collection<Item> getAllItemsByItemId(int itemId) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (itemId == item.getId()) {
            result.add(item);
         }
      }

      return result;
   }

   public Item getItemByItemId(int itemId, Item itemToIgnore) {
      for (Item item : this._items) {
         if (item.getId() == itemId && !item.equals(itemToIgnore)) {
            return item;
         }
      }

      return null;
   }

   public Item getItemByObjectId(int objectId) {
      for (Item item : this._items) {
         if (objectId == item.getObjectId()) {
            return item;
         }
      }

      return null;
   }

   public long getInventoryItemCount(int itemId, int enchantLevel) {
      return this.getInventoryItemCount(itemId, enchantLevel, true);
   }

   public long getInventoryItemCount(int itemId, int enchantLevel, boolean includeEquipped) {
      long count = 0L;

      for (Item item : this._items) {
         if (item.getId() == itemId && (item.getEnchantLevel() == enchantLevel || enchantLevel < 0) && (includeEquipped || !item.isEquipped())) {
            if (item.isStackable()) {
               return item.getCount();
            }

            count++;
         }
      }

      return count;
   }

   public Item addItem(ItemProcessType process, Item item, Player actor, Object reference) {
      Item newItem = item;
      Item olditem = this.getItemByItemId(item.getId());
      if (olditem != null && olditem.isStackable()) {
         long count = item.getCount();
         olditem.changeCount(process, count, actor, reference);
         olditem.setLastChange(2);
         ItemManager.destroyItem(process, item, actor, reference);
         item.updateDatabase();
         newItem = olditem;
      } else {
         item.setOwnerId(process, this.getOwnerId(), actor, reference);
         item.setItemLocation(this.getBaseLocation());
         item.setLastChange(1);
         this.addItem(item);
      }

      this.refreshWeight();
      return newItem;
   }

   public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference) {
      Item item = this.getItemByItemId(itemId);
      if (item != null && item.isStackable()) {
         item.changeCount(process, count, actor, reference);
         item.setLastChange(2);
      } else {
         InventoryUpdate iu = new InventoryUpdate();
         int i = 0;

         while (true) {
            if (i < count) {
               ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
               if (template == null) {
                  LOGGER.warning("Invalid ItemId (" + itemId + ") requested by " + (actor != null ? actor : process));
                  return null;
               }

               item = ItemManager.createItem(process, itemId, template.isStackable() ? count : 1L, actor, reference);
               item.setOwnerId(this.getOwnerId());
               item.setItemLocation(this.getBaseLocation());
               item.setLastChange(1);
               item.setEnchantLevel(template.getDefaultEnchantLevel());
               this.addItem(item);
               if (count > 1L && i < count - 1L) {
                  iu.addNewItem(item);
               }

               if (!template.isStackable() && GeneralConfig.MULTIPLE_ITEM_DROP) {
                  i++;
                  continue;
               }
            }

            if (count > 1L && item != null && !item.isStackable() && item.getItemLocation() == ItemLocation.INVENTORY) {
               actor.sendInventoryUpdate(iu);
            }
            break;
         }
      }

      this.refreshWeight();
      return item;
   }

   public Item transferItem(ItemProcessType process, int objectId, long countValue, ItemContainer target, Player actor, Object reference) {
      if (target == null) {
         return null;
      } else {
         Item sourceitem = this.getItemByObjectId(objectId);
         if (sourceitem == null) {
            return null;
         } else {
            Item targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getId()) : null;
            synchronized (sourceitem) {
               if (this.getItemByObjectId(objectId) != sourceitem) {
                  return null;
               } else {
                  long count = countValue;
                  if (countValue > sourceitem.getCount()) {
                     count = sourceitem.getCount();
                  }

                  if (sourceitem.getCount() == count && targetitem == null && !sourceitem.isStackable()) {
                     this.removeItem(sourceitem);
                     target.addItem(process, sourceitem, actor, reference);
                     targetitem = sourceitem;
                  } else {
                     if (sourceitem.getCount() > count) {
                        sourceitem.changeCount(process, -count, actor, reference);
                     } else {
                        this.removeItem(sourceitem);
                        ItemManager.destroyItem(process, sourceitem, actor, reference);
                     }

                     if (targetitem != null) {
                        targetitem.changeCount(process, count, actor, reference);
                     } else {
                        targetitem = target.addItem(process, sourceitem.getId(), count, actor, reference);
                     }
                  }

                  sourceitem.updateDatabase(true);
                  if (targetitem != sourceitem && targetitem != null) {
                     targetitem.updateDatabase();
                  }

                  if (sourceitem.isAugmented()) {
                     sourceitem.getAugmentation().removeBonus(actor);
                  }

                  this.refreshWeight();
                  target.refreshWeight();
                  return targetitem;
               }
            }
         }
      }
   }

   public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference) {
      return this.destroyItem(process, item, item.getCount(), actor, reference);
   }

   public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference) {
      synchronized (item) {
         if (item.getCount() > count) {
            item.changeCount(process, -count, actor, reference);
            item.setLastChange(2);
            this.refreshWeight();
         } else {
            if (item.getCount() < count) {
               return null;
            }

            boolean removed = this.removeItem(item);
            if (!removed) {
               return null;
            }

            ItemManager.destroyItem(process, item, actor, reference);
            item.updateDatabase();
            this.refreshWeight();
            item.stopAllTasks();
         }

         return item;
      }
   }

   public Item destroyItem(ItemProcessType process, int objectId, long count, Player actor, Object reference) {
      Item item = this.getItemByObjectId(objectId);
      return item == null ? null : this.destroyItem(process, item, count, actor, reference);
   }

   public Item destroyItemByItemId(ItemProcessType process, int itemId, long count, Player actor, Object reference) {
      Item item = this.getItemByItemId(itemId);
      return item == null ? null : this.destroyItem(process, item, count, actor, reference);
   }

   public void destroyAllItems(ItemProcessType process, Player actor, Object reference) {
      for (Item item : this._items) {
         this.destroyItem(process, item, actor, reference);
      }
   }

   public long getAdena() {
      for (Item item : this._items) {
         if (item.getId() == 57) {
            return item.getCount();
         }
      }

      return 0L;
   }

   protected void addItem(Item item) {
      this._items.add(item);
   }

   protected boolean removeItem(Item item) {
      return this._items.remove(item);
   }

   protected void refreshWeight() {
   }

   public void deleteMe() {
      if (this instanceof PlayerInventory || this instanceof PlayerWarehouse || this.getOwner() != null) {
         for (Item item : this._items) {
            item.updateDatabase(true);
            item.stopAllTasks();
            ItemVariables vars = item.getScript(ItemVariables.class);
            if (vars != null) {
               vars.saveNow();
            }
         }
      }

      for (Item itemx : this._items) {
         World.removeObject(itemx);
      }

      this._items.clear();
   }

   public void updateDatabase() {
      if (this.getOwner() != null) {
         for (Item item : this._items) {
            item.updateDatabase(true);
         }
      }
   }

   public void restore() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=?)"
         );
      ) {
         ps.setInt(1, this.getOwnerId());
         ps.setString(2, this.getBaseLocation().name());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Item item = Item.restoreFromDb(this.getOwnerId(), rs);
               if (item != null) {
                  World.addObject(item);
                  Player owner = this.getOwner() != null ? this.getOwner().asPlayer() : null;
                  if (item.isStackable() && this.getItemByItemId(item.getId()) != null) {
                     this.addItem(ItemProcessType.RESTORE, item, owner, null);
                  } else {
                     this.addItem(item);
                  }
               }
            }
         }

         this.refreshWeight();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore container:", (Throwable)var12);
      }
   }

   public boolean validateCapacity(long slots) {
      return true;
   }

   public boolean validateWeight(long weight) {
      return true;
   }

   public boolean validateCapacityByItemId(int itemId, long count) {
      ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
      return template == null || (template.isStackable() ? this.validateCapacity(1L) : this.validateCapacity(count));
   }

   public boolean validateWeightByItemId(int itemId, long count) {
      ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
      return template == null || this.validateWeight(template.getWeight() * count);
   }
}
