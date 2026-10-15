package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public abstract class AbstractInventoryUpdate extends AbstractItemPacket {
   private final Map<Integer, ItemInfo> _items = new HashMap<>();

   public AbstractInventoryUpdate() {
   }

   public AbstractInventoryUpdate(Item item) {
      this.addItem(item);
   }

   public AbstractInventoryUpdate(List<ItemInfo> items) {
      synchronized (this._items) {
         for (ItemInfo item : items) {
            this._items.put(item.getObjectId(), item);
         }
      }
   }

   public void addItem(Item item) {
      synchronized (this._items) {
         this._items.put(item.getObjectId(), new ItemInfo(item));
      }
   }

   public void addNewItem(Item item) {
      synchronized (this._items) {
         this._items.put(item.getObjectId(), new ItemInfo(item, 1));
      }
   }

   public void addModifiedItem(Item item) {
      synchronized (this._items) {
         this._items.put(item.getObjectId(), new ItemInfo(item, 2));
      }
   }

   public void addRemovedItem(Item item) {
      synchronized (this._items) {
         this._items.put(item.getObjectId(), new ItemInfo(item, 3));
      }
   }

   public void addItems(Collection<Item> items) {
      synchronized (this._items) {
         for (Item item : items) {
            this._items.put(item.getObjectId(), new ItemInfo(item));
         }
      }
   }

   public void putAll(Map<Integer, ItemInfo> items) {
      synchronized (this._items) {
         this._items.putAll(items);
      }
   }

   public Map<Integer, ItemInfo> getItemEntries() {
      return this._items;
   }

   public Collection<ItemInfo> getItems() {
      return this._items.values();
   }

   protected void writeItems(WriteBuffer buffer) {
      synchronized (this._items) {
         buffer.writeShort(this._items.size());

         for (ItemInfo item : this._items.values()) {
            buffer.writeShort(item.getChange());
            this.writeItem(item, buffer);
         }

         this._items.clear();
      }
   }
}
