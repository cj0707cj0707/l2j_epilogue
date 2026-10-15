package org.l2jmobius.gameserver.entity.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemDrop;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemTransfer;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.util.ArrayUtil;

public class PlayerInventory extends Inventory {
   private static final Logger LOGGER = Logger.getLogger(PlayerInventory.class.getName());
   private final Player _owner;
   private Item _adena;
   private Item _ancientAdena;
   private int[] _blockItems = null;
   private int _blockMode = -1;

   public PlayerInventory(Player owner) {
      this._owner = owner;
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   protected ItemLocation getBaseLocation() {
      return ItemLocation.INVENTORY;
   }

   @Override
   protected ItemLocation getEquipLocation() {
      return ItemLocation.PAPERDOLL;
   }

   public Item getAdenaInstance() {
      return this._adena;
   }

   @Override
   public long getAdena() {
      return this._adena != null ? this._adena.getCount() : 0L;
   }

   public Item getAncientAdenaInstance() {
      return this._ancientAdena;
   }

   public long getAncientAdena() {
      return this._ancientAdena != null ? this._ancientAdena.getCount() : 0L;
   }

   public Collection<Item> getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable, boolean checkSellable) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if ((allowAdena || item.getId() != 57) && (allowAncientAdena || item.getId() != 5575)) {
            boolean isDuplicate = false;

            for (Item addedItem : result) {
               if (addedItem.getId() == item.getId()) {
                  isDuplicate = true;
                  break;
               }
            }

            if (!isDuplicate && (!onlyAvailable || (!checkSellable || item.isSellable()) && item.isAvailable(this._owner, false, false))) {
               result.add(item);
            }
         }
      }

      return result;
   }

   public Collection<Item> getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (item != null && (allowAdena || item.getId() != 57) && (allowAncientAdena || item.getId() != 5575)) {
            boolean isDuplicate = false;

            for (Item addedItem : result) {
               if (addedItem.getId() == item.getId() && addedItem.getEnchantLevel() == item.getEnchantLevel()) {
                  isDuplicate = true;
                  break;
               }
            }

            if (!isDuplicate && (!onlyAvailable || item.isSellable() && item.isAvailable(this._owner, false, false))) {
               result.add(item);
            }
         }
      }

      return result;
   }

   public Collection<Item> getAllItemsByItemId(int itemId, boolean includeEquipped) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (itemId == item.getId() && (includeEquipped || !item.isEquipped())) {
            result.add(item);
         }
      }

      return result;
   }

   public Collection<Item> getAllItemsByItemId(int itemId, int enchantment) {
      return this.getAllItemsByItemId(itemId, enchantment, true);
   }

   public Collection<Item> getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (itemId == item.getId() && item.getEnchantLevel() == enchantment && (includeEquipped || !item.isEquipped())) {
            result.add(item);
         }
      }

      return result;
   }

   public Collection<Item> getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable) {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (item.isAvailable(this._owner, allowAdena, allowNonTradeable) && this.canManipulateWithItemId(item.getId())) {
            if (feightable) {
               if (item.getItemLocation() == ItemLocation.INVENTORY && item.isFreightable()) {
                  result.add(item);
               }
            } else {
               result.add(item);
            }
         }
      }

      return result;
   }

   public Collection<Item> getAugmentedItems() {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (item.isAugmented()) {
            result.add(item);
         }
      }

      return result;
   }

   public Collection<Item> getElementItems() {
      Collection<Item> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (item.getElementals() != null) {
            result.add(item);
         }
      }

      return result;
   }

   public Collection<TradeItem> getAvailableItems(TradeList tradeList) {
      Collection<TradeItem> result = new ArrayDeque<>();

      for (Item item : this._items) {
         if (item != null && item.isAvailable(this._owner, false, false)) {
            TradeItem adjItem = tradeList.adjustAvailableItem(item);
            if (adjItem != null) {
               result.add(adjItem);
            }
         }
      }

      return result;
   }

   public void adjustAvailableItem(TradeItem item) {
      boolean notAllEquipped = false;

      for (Item adjItem : this.getAllItemsByItemId(item.getItem().getId())) {
         if (!adjItem.isEquipable()) {
            notAllEquipped |= true;
            break;
         }

         if (!adjItem.isEquipped()) {
            notAllEquipped |= true;
         }
      }

      if (notAllEquipped) {
         Item adjItem = this.getItemByItemId(item.getItem().getId());
         item.setObjectId(adjItem.getObjectId());
         item.setEnchant(adjItem.getEnchantLevel());
         if (adjItem.getCount() < item.getCount()) {
            item.setCount(adjItem.getCount());
         }
      } else {
         item.setCount(0L);
      }
   }

   public void addAdena(ItemProcessType process, long count, Player actor, Object reference) {
      if (count > 0L) {
         this.addItem(process, 57, count, actor, reference);
      }
   }

   public boolean reduceAdena(ItemProcessType process, long count, Player actor, Object reference) {
      return count > 0L ? this.destroyItemByItemId(process, 57, count, actor, reference) != null : false;
   }

   public void addAncientAdena(ItemProcessType process, long count, Player actor, Object reference) {
      if (count > 0L) {
         this.addItem(process, 5575, count, actor, reference);
      }
   }

   public boolean reduceAncientAdena(ItemProcessType process, long count, Player actor, Object reference) {
      return count > 0L && this.destroyItemByItemId(process, 5575, count, actor, reference) != null;
   }

   @Override
   public Item addItem(ItemProcessType process, Item item, Player actor, Object reference) {
      Item addedItem = super.addItem(process, item, actor, reference);
      if (addedItem != null) {
         if (addedItem.getId() == 57 && !addedItem.equals(this._adena)) {
            this._adena = addedItem;
         } else if (addedItem.getId() == 5575 && !addedItem.equals(this._ancientAdena)) {
            this._ancientAdena = addedItem;
         }

         if (actor != null && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, addedItem), actor);
         }
      }

      return addedItem;
   }

   @Override
   public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference) {
      Item item = super.addItem(process, itemId, count, actor, reference);
      if (item != null) {
         if (item.getId() == 57 && !item.equals(this._adena)) {
            this._adena = item;
         } else if (item.getId() == 5575 && !item.equals(this._ancientAdena)) {
            this._ancientAdena = item;
         }

         if (actor != null) {
            InventoryUpdate playerIU = new InventoryUpdate();
            if (item.isStackable() && item.getCount() > count) {
               playerIU.addModifiedItem(item);
            } else {
               playerIU.addNewItem(item);
            }

            actor.sendInventoryUpdate(playerIU);
            StatusUpdate su = new StatusUpdate(actor);
            su.addAttribute(14, actor.getCurrentLoad());
            actor.sendPacket(su);
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), actor);
            }
         }
      }

      return item;
   }

   @Override
   public Item transferItem(ItemProcessType process, int objectId, long count, ItemContainer target, Player actor, Object reference) {
      Item item = super.transferItem(process, objectId, count, target, actor, reference);
      if (this._adena != null && (this._adena.getCount() <= 0L || this._adena.getOwnerId() != this.getOwnerId())) {
         this._adena = null;
      }

      if (this._ancientAdena != null && (this._ancientAdena.getCount() <= 0L || this._ancientAdena.getOwnerId() != this.getOwnerId())) {
         this._ancientAdena = null;
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_TRANSFER, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemTransfer(actor, item, target), item.getTemplate());
      }

      return item;
   }

   @Override
   public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference) {
      return this.destroyItem(process, item, item.getCount(), actor, reference);
   }

   @Override
   public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference) {
      Item destroyedItem = super.destroyItem(process, item, count, actor, reference);
      if (this._adena != null && this._adena.getCount() <= 0L) {
         this._adena = null;
      }

      if (this._ancientAdena != null && this._ancientAdena.getCount() <= 0L) {
         this._ancientAdena = null;
      }

      if (destroyedItem != null && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DESTROY, destroyedItem.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDestroy(actor, destroyedItem), destroyedItem.getTemplate());
      }

      return destroyedItem;
   }

   @Override
   public Item destroyItem(ItemProcessType process, int objectId, long count, Player actor, Object reference) {
      Item item = this.getItemByObjectId(objectId);
      return item == null ? null : this.destroyItem(process, item, count, actor, reference);
   }

   @Override
   public Item destroyItemByItemId(ItemProcessType process, int itemId, long count, Player actor, Object reference) {
      Item destroyItem = null;
      Collection<Item> items = this.getAllItemsByItemId(itemId);

      for (Item item : items) {
         destroyItem = item;
         if (!item.isEquipped()) {
            break;
         }
      }

      if (destroyItem == null) {
         return null;
      } else {
         if (!destroyItem.isStackable() && count > 1L) {
            if (this.getInventoryItemCount(itemId, -1, false) < count) {
               return null;
            }

            InventoryUpdate iu = new InventoryUpdate();
            long destroyed = 0L;

            for (Item itemx : items) {
               if (!itemx.isEquipped() && this.destroyItem(process, itemx, 1L, actor, reference) != null) {
                  iu.addRemovedItem(itemx);
                  if (++destroyed == count) {
                     this._owner.sendInventoryUpdate(iu);
                     this.refreshWeight();
                     return itemx;
                  }
               }
            }
         }

         return this.destroyItem(process, destroyItem, count, actor, reference);
      }
   }

   @Override
   public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference) {
      Item droppedItem = super.dropItem(process, item, actor, reference);
      if (this._adena != null && (this._adena.getCount() <= 0L || this._adena.getOwnerId() != this.getOwnerId())) {
         this._adena = null;
      }

      if (this._ancientAdena != null && (this._ancientAdena.getCount() <= 0L || this._ancientAdena.getOwnerId() != this.getOwnerId())) {
         this._ancientAdena = null;
      }

      if (droppedItem != null && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, droppedItem.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, droppedItem, droppedItem.getLocation()), droppedItem.getTemplate());
      }

      return droppedItem;
   }

   @Override
   public Item dropItem(ItemProcessType process, int objectId, long count, Player actor, Object reference) {
      Item item = super.dropItem(process, objectId, count, actor, reference);
      if (this._adena != null && (this._adena.getCount() <= 0L || this._adena.getOwnerId() != this.getOwnerId())) {
         this._adena = null;
      }

      if (this._ancientAdena != null && (this._ancientAdena.getCount() <= 0L || this._ancientAdena.getOwnerId() != this.getOwnerId())) {
         this._ancientAdena = null;
      }

      if (item != null && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getTemplate());
      }

      return item;
   }

   @Override
   protected void addItem(Item item) {
      super.addItem(item);
   }

   @Override
   protected boolean removeItem(Item item) {
      this._owner.removeItemFromShortcut(item.getObjectId());
      if (item.getObjectId() == this._owner.getActiveEnchantItemId()) {
         this._owner.setActiveEnchantItemId(-1);
      }

      if (item.getId() == 57) {
         this._adena = null;
      } else if (item.getId() == 5575) {
         this._ancientAdena = null;
      }

      return super.removeItem(item);
   }

   public int getNonQuestSize() {
      return this._items.size();
   }

   @Override
   public void refreshWeight() {
      super.refreshWeight();
      this._owner.refreshOverloaded();
   }

   @Override
   public void restore() {
      super.restore();
      this._adena = this.getItemByItemId(57);
      this._ancientAdena = this.getItemByItemId(5575);
   }

   public static int[][] restoreVisibleInventory(int objectId) {
      int[][] paperdoll = new int[31][3];

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
      ) {
         ps.setInt(1, objectId);

         try (ResultSet invdata = ps.executeQuery()) {
            while (invdata.next()) {
               int slot = invdata.getInt("loc_data");
               paperdoll[slot][0] = invdata.getInt("object_id");
               if (TransmogConfig.ENABLE_TRANSMOG) {
                  ItemVariables vars = new ItemVariables(paperdoll[slot][0]);
                  paperdoll[slot][1] = vars.getInt("transmogId", invdata.getInt("item_id"));
               } else {
                  int itemId = invdata.getInt("item_id");
                  ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
                  paperdoll[slot][1] = template == null ? itemId : template.getDisplayId();
               }

               paperdoll[slot][2] = invdata.getInt("enchant_level");
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Could not restore inventory: " + var14.getMessage(), (Throwable)var14);
      }

      return paperdoll;
   }

   public boolean checkInventorySlotsAndWeight(List<ItemTemplate> itemList, boolean sendMessage, boolean sendSkillMessage) {
      int lootWeight = 0;
      int requiredSlots = 0;
      if (itemList != null) {
         for (ItemTemplate item : itemList) {
            if (!item.isStackable() || this.getInventoryItemCount(item.getId(), -1) <= 0L) {
               requiredSlots++;
            }

            lootWeight += item.getWeight();
         }
      }

      boolean inventoryStatusOK = this.validateCapacity(requiredSlots) && this.validateWeight(lootWeight);
      if (!inventoryStatusOK && sendMessage) {
         this._owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
         if (sendSkillMessage) {
            this._owner.sendPacket(SystemMessageId.WEIGHT_AND_VOLUME_LIMIT_HAS_BEEN_EXCEEDED_THAT_SKILL_IS_CURRENTLY_UNAVAILABLE);
         }
      }

      return inventoryStatusOK;
   }

   public boolean validateCapacity(Item item) {
      int slots = 0;
      if (!item.isStackable() || this.getInventoryItemCount(item.getId(), -1) <= 0L && !item.getTemplate().hasExImmediateEffect()) {
         slots++;
      }

      return this.validateCapacity(slots, item.isQuestItem());
   }

   public boolean validateCapacityByItemId(int itemId) {
      int slots = 0;
      Item invItem = this.getItemByItemId(itemId);
      if (invItem == null || !invItem.isStackable()) {
         slots++;
      }

      return this.validateCapacity(slots, ItemData.getInstance().getTemplate(itemId).isQuestItem());
   }

   @Override
   public boolean validateCapacity(long slots) {
      return this.validateCapacity(slots, false);
   }

   public boolean validateCapacity(long slots, boolean questItem) {
      return slots == 0L && !PlayerConfig.AUTO_LOOT_SLOT_LIMIT || this.getNonQuestSize() + slots <= this._owner.getInventoryLimit();
   }

   @Override
   public boolean validateWeight(long weight) {
      return this._owner.isGM() && this._owner.getDietMode() && this._owner.getAccessLevel().allowTransaction()
         ? true
         : this._totalWeight + weight <= this._owner.getMaxLoad();
   }

   public void setInventoryBlock(int[] items, int mode) {
      this._blockMode = mode;
      this._blockItems = items;
      this._owner.sendItemList(false);
   }

   public void unblock() {
      this._blockMode = -1;
      this._blockItems = null;
      this._owner.sendItemList(false);
   }

   public boolean hasInventoryBlock() {
      return this._blockMode > -1 && this._blockItems != null && this._blockItems.length > 0;
   }

   public void blockAllItems() {
      this.setInventoryBlock(new int[]{ItemData.getInstance().getArraySize() + 2}, 1);
   }

   public int getBlockMode() {
      return this._blockMode;
   }

   public int[] getBlockItems() {
      return this._blockItems;
   }

   public boolean canManipulateWithItemId(int itemId) {
      return (this._blockMode != 0 || !ArrayUtil.contains(this._blockItems, itemId)) && (this._blockMode != 1 || ArrayUtil.contains(this._blockItems, itemId));
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[" + this._owner + "]";
   }

   public void applyItemSkills() {
      for (Item item : this._items) {
         item.giveSkillsToOwner();
         item.applyEnchantStats();
      }
   }
}
