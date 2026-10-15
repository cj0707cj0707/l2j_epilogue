package org.l2jmobius.gameserver.network.holders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class TradeList {
   private static final Logger LOGGER = Logger.getLogger(TradeList.class.getName());
   private final Player _owner;
   private Player _partner;
   private final Set<TradeItem> _items = ConcurrentHashMap.newKeySet();
   private String _title;
   private boolean _packaged;
   private boolean _confirmed = false;
   private boolean _locked = false;

   public TradeList(Player owner) {
      this._owner = owner;
   }

   public Player getOwner() {
      return this._owner;
   }

   public void setPartner(Player partner) {
      this._partner = partner;
   }

   public Player getPartner() {
      return this._partner;
   }

   public void setTitle(String title) {
      this._title = title;
   }

   public String getTitle() {
      return this._title;
   }

   public boolean isLocked() {
      return this._locked;
   }

   public boolean isConfirmed() {
      return this._confirmed;
   }

   public boolean isPackaged() {
      return this._packaged;
   }

   public void setPackaged(boolean value) {
      this._packaged = value;
   }

   public Collection<TradeItem> getItems() {
      return this._items;
   }

   public Collection<TradeItem> getAvailableItems(PlayerInventory inventory) {
      List<TradeItem> list = new ArrayList<>();

      for (TradeItem item : this._items) {
         item = new TradeItem(item, item.getCount(), item.getPrice());
         inventory.adjustAvailableItem(item);
         list.add(item);
      }

      return list;
   }

   public int getItemCount() {
      return this._items.size();
   }

   public TradeItem adjustAvailableItem(Item item) {
      if (item.isStackable()) {
         for (TradeItem exclItem : this._items) {
            if (exclItem.getItem().getId() == item.getId()) {
               return item.getCount() <= exclItem.getCount()
                  ? null
                  : new TradeItem(item, item.getCount() - exclItem.getCount(), (long)item.getReferencePrice());
            }
         }
      }

      return new TradeItem(item, item.getCount(), (long)item.getReferencePrice());
   }

   public TradeItem addItem(int objectId, long count) {
      return this.addItem(objectId, count, 0L);
   }

   public synchronized TradeItem addItem(int objectId, long count, long price) {
      if (this._locked) {
         return null;
      } else if (!(World.findObject(objectId) instanceof Item item)) {
         return null;
      } else if ((item.isTradeable() || this._owner.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS) && !item.isQuestItem()) {
         if (!this._owner.getInventory().canManipulateWithItemId(item.getId())) {
            return null;
         } else if (count > 0L && count <= item.getCount()) {
            if (!item.isStackable() && count > 1L) {
               return null;
            } else if (Inventory.MAX_ADENA / count < price) {
               return null;
            } else {
               for (TradeItem checkitem : this._items) {
                  if (checkitem.getObjectId() == objectId) {
                     return null;
                  }
               }

               TradeItem titem = new TradeItem(item, count, price);
               this._items.add(titem);
               this.invalidateConfirmation();
               return titem;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public synchronized TradeItem addItemByItemId(int itemId, long count, long price) {
      if (this._locked) {
         LOGGER.warning(this._owner.getName() + ": Attempt to modify locked TradeList!");
         return null;
      } else {
         ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
         if (item == null) {
            LOGGER.warning(this._owner.getName() + ": Attempt to add invalid item to TradeList!");
            return null;
         } else if (!item.isTradeable() || item.isQuestItem()) {
            return null;
         } else if (!item.isStackable() && count > 1L) {
            LOGGER.warning(this._owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
            return null;
         } else if (Inventory.MAX_ADENA / count < price) {
            LOGGER.warning(this._owner.getName() + ": Attempt to overflow adena !");
            return null;
         } else {
            TradeItem titem = new TradeItem(item, count, price);
            this._items.add(titem);
            this.invalidateConfirmation();
            return titem;
         }
      }
   }

   private synchronized TradeItem removeItem(int objectId, int itemId, long count) {
      if (this._locked) {
         LOGGER.warning(this._owner.getName() + ": Attempt to modify locked TradeList!");
         return null;
      } else {
         for (TradeItem titem : this._items) {
            if (titem.getObjectId() == objectId || titem.getItem().getId() == itemId) {
               if (this._partner != null) {
                  TradeList partnerList = this._partner.getActiveTradeList();
                  if (partnerList == null) {
                     LOGGER.warning(this._partner.getName() + ": Trading partner (" + this._partner.getName() + ") is invalid in this trade!");
                     return null;
                  }

                  partnerList.invalidateConfirmation();
               }

               if (count != -1L && titem.getCount() > count) {
                  titem.setCount(titem.getCount() - count);
               } else {
                  this._items.remove(titem);
               }

               return titem;
            }
         }

         return null;
      }
   }

   public synchronized void updateItems() {
      for (TradeItem titem : this._items) {
         Item item = this._owner.getInventory().getItemByObjectId(titem.getObjectId());
         if (item == null || titem.getCount() < 1L) {
            this.removeItem(titem.getObjectId(), -1, -1L);
         } else if (item.getCount() < titem.getCount()) {
            titem.setCount(item.getCount());
         }
      }
   }

   public void lock() {
      this._locked = true;
   }

   public synchronized void clear() {
      this._items.clear();
      this._locked = false;
   }

   public boolean confirm() {
      if (this._confirmed) {
         return true;
      } else if (this._partner != null) {
         TradeList partnerList = this._partner.getActiveTradeList();
         if (partnerList == null) {
            LOGGER.warning(this._partner.getName() + ": Trading partner (" + this._partner.getName() + ") is invalid in this trade!");
            return false;
         } else {
            TradeList sync1;
            TradeList sync2;
            if (this.getOwner().getObjectId() > partnerList.getOwner().getObjectId()) {
               sync1 = partnerList;
               sync2 = this;
            } else {
               sync1 = this;
               sync2 = partnerList;
            }

            synchronized (sync1) {
               boolean var10000;
               synchronized (sync2) {
                  this._confirmed = true;
                  if (partnerList.isConfirmed()) {
                     partnerList.lock();
                     this.lock();
                     if (!partnerList.validate() || !this.validate()) {
                        var10000 = false;
                     } else {
                        this.doExchange(partnerList);
                        return this._confirmed;
                     }
                  } else {
                     this._partner.onTradeConfirm(this._owner);
                     return this._confirmed;
                  }
               }

               return var10000;
            }
         }
      } else {
         this._confirmed = true;
         return this._confirmed;
      }
   }

   private void invalidateConfirmation() {
      this._confirmed = false;
   }

   private boolean validate() {
      if (this._owner != null && World.getPlayer(this._owner.getObjectId()) != null) {
         for (TradeItem titem : this._items) {
            Item item = this._owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
            if (item == null || item.getCount() < 1L) {
               LOGGER.warning(this._owner.getName() + ": Invalid Item in TradeList");
               return false;
            }
         }

         return true;
      } else {
         LOGGER.warning("Invalid owner of TradeList");
         return false;
      }
   }

   private boolean TransferItems(Player partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU) {
      for (TradeItem titem : this._items) {
         Item oldItem = this._owner.getInventory().getItemByObjectId(titem.getObjectId());
         if (oldItem == null) {
            return false;
         }

         Item newItem = this._owner
            .getInventory()
            .transferItem(ItemProcessType.TRANSFER, titem.getObjectId(), titem.getCount(), partner.getInventory(), this._owner, this._partner);
         if (newItem == null) {
            return false;
         }

         if (ownerIU != null) {
            if (oldItem.getCount() > 0L && oldItem != newItem) {
               ownerIU.addModifiedItem(oldItem);
            } else {
               ownerIU.addRemovedItem(oldItem);
            }
         }

         if (partnerIU != null) {
            if (newItem.getCount() > titem.getCount()) {
               partnerIU.addModifiedItem(newItem);
            } else {
               partnerIU.addNewItem(newItem);
            }
         }
      }

      return true;
   }

   private int countItemsSlots(Player partner) {
      int slots = 0;

      for (TradeItem item : this._items) {
         if (item != null) {
            ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
            if (template != null) {
               if (!template.isStackable()) {
                  slots = (int)(slots + item.getCount());
               } else if (partner.getInventory().getItemByItemId(item.getItem().getId()) == null) {
                  slots++;
               }
            }
         }
      }

      return slots;
   }

   private int calcItemsWeight() {
      long weight = 0L;

      for (TradeItem item : this._items) {
         if (item != null) {
            ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
            if (template != null) {
               weight += item.getCount() * template.getWeight();
            }
         }
      }

      return (int)Math.min(weight, 2147483647L);
   }

   private void doExchange(TradeList partnerList) {
      boolean success = false;
      if (!this.getOwner().getInventory().validateWeight(partnerList.calcItemsWeight())
         || !partnerList.getOwner().getInventory().validateWeight(this.calcItemsWeight())) {
         partnerList.getOwner().sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
         this._owner.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
      } else if (this.getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(this.getOwner()))
         && partnerList.getOwner().getInventory().validateCapacity(this.countItemsSlots(partnerList.getOwner()))) {
         InventoryUpdate ownerIU = new InventoryUpdate();
         InventoryUpdate partnerIU = new InventoryUpdate();
         partnerList.TransferItems(this._owner, partnerIU, ownerIU);
         this.TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
         this._owner.sendInventoryUpdate(ownerIU);
         this._partner.sendInventoryUpdate(partnerIU);
         StatusUpdate playerSU = new StatusUpdate(this._owner);
         playerSU.addAttribute(14, this._owner.getCurrentLoad());
         this._owner.sendPacket(playerSU);
         playerSU = new StatusUpdate(this._partner);
         playerSU.addAttribute(14, this._partner.getCurrentLoad());
         this._partner.sendPacket(playerSU);
         success = true;
      } else {
         partnerList.getOwner().sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
         this._owner.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
      }

      partnerList.getOwner().onTradeFinish(success);
      this._owner.onTradeFinish(success);
   }

   public synchronized int privateStoreBuy(Player player, Set<RequestTrade> items) {
      if (this._locked) {
         return 1;
      } else if (!this.validate()) {
         this.lock();
         return 1;
      } else if (this._owner.isOnline() && player.isOnline()) {
         int slots = 0;
         int weight = 0;
         long totalPrice = 0L;
         PlayerInventory ownerInventory = this._owner.getInventory();
         PlayerInventory playerInventory = player.getInventory();

         for (RequestTrade item : items) {
            boolean found = false;

            for (TradeItem ti : this._items) {
               if (ti.getObjectId() == item.getObjectId()) {
                  if (ti.getPrice() == item.getPrice()) {
                     if (ti.getCount() < item.getCount()) {
                        item.setCount(ti.getCount());
                     }

                     found = true;
                  }
                  break;
               }
            }

            if (found) {
               if (Inventory.MAX_ADENA / item.getCount() < item.getPrice()) {
                  this.lock();
                  return 1;
               }

               totalPrice += item.getCount() * item.getPrice();
               if (Inventory.MAX_ADENA >= totalPrice && totalPrice >= 0L) {
                  Item oldItem = this._owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
                  if (oldItem != null && oldItem.isTradeable()) {
                     ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
                     if (template != null) {
                        weight = (int)(weight + item.getCount() * template.getWeight());
                        if (!template.isStackable()) {
                           slots = (int)(slots + item.getCount());
                        } else if (playerInventory.getItemByItemId(item.getItemId()) == null) {
                           slots++;
                        }
                     }
                     continue;
                  }

                  this.lock();
                  return 2;
               }

               this.lock();
               return 1;
            } else {
               if (this._packaged) {
                  PunishmentManager.handleIllegalPlayerAction(
                     player,
                     "[TradeList.privateStoreBuy()] "
                        + player
                        + " tried to cheat the package sell and buy only a part of the package! Ban this player for bot usage!",
                     GeneralConfig.DEFAULT_PUNISH
                  );
                  return 2;
               }

               item.setCount(0L);
            }
         }

         if (totalPrice > playerInventory.getAdena()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            return 1;
         } else if (!playerInventory.validateWeight(weight)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
            return 1;
         } else if (!playerInventory.validateCapacity(slots)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
            return 1;
         } else if (!playerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, player, this._owner)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            return 1;
         } else {
            InventoryUpdate ownerIU = new InventoryUpdate();
            InventoryUpdate playerIU = new InventoryUpdate();
            Item adenaItem = playerInventory.getAdenaInstance();
            playerIU.addItem(adenaItem);
            ownerInventory.addAdena(ItemProcessType.SELL, totalPrice, this._owner, player);
            boolean ok = true;

            for (RequestTrade item : items) {
               if (item.getCount() != 0L) {
                  Item oldItem = this._owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
                  if (oldItem == null) {
                     this.lock();
                     ok = false;
                     break;
                  }

                  Item newItem = ownerInventory.transferItem(
                     ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), playerInventory, this._owner, player
                  );
                  if (newItem == null) {
                     ok = false;
                     break;
                  }

                  this.removeItem(item.getObjectId(), -1, item.getCount());
                  if (oldItem.getCount() > 0L && oldItem != newItem) {
                     ownerIU.addModifiedItem(oldItem);
                  } else {
                     ownerIU.addRemovedItem(oldItem);
                  }

                  if (newItem.getCount() > item.getCount()) {
                     playerIU.addModifiedItem(newItem);
                  } else {
                     playerIU.addNewItem(newItem);
                  }

                  SystemMessage var25;
                  if (newItem.isStackable()) {
                     SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_PURCHASED_S3_S2_S);
                     msg.addString(player.getName());
                     msg.addItemName(newItem);
                     msg.addLong(item.getCount());
                     this._owner.sendPacket(msg);
                     var25 = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_C1);
                     var25.addString(this._owner.getName());
                     var25.addItemName(newItem);
                     var25.addLong(item.getCount());
                  } else {
                     SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_PURCHASED_S2);
                     msg.addString(player.getName());
                     msg.addItemName(newItem);
                     this._owner.sendPacket(msg);
                     var25 = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_C1);
                     var25.addString(this._owner.getName());
                     var25.addItemName(newItem);
                  }

                  player.sendPacket(var25);
               }
            }

            this._owner.sendInventoryUpdate(ownerIU);
            player.sendInventoryUpdate(playerIU);
            return ok ? 0 : 2;
         }
      } else {
         return 1;
      }
   }

   public synchronized boolean privateStoreSell(Player player, RequestTrade[] items) {
      if (!this._locked && this._owner.isOnline() && player.isOnline()) {
         boolean ok = false;
         PlayerInventory ownerInventory = this._owner.getInventory();
         PlayerInventory playerInventory = player.getInventory();
         InventoryUpdate ownerIU = new InventoryUpdate();
         InventoryUpdate playerIU = new InventoryUpdate();
         long totalPrice = 0L;

         for (RequestTrade item : items) {
            boolean found = false;

            for (TradeItem ti : this._items) {
               if (ti.getItem().getId() == item.getItemId()) {
                  if (ti.getPrice() == item.getPrice()) {
                     if (ti.getCount() < item.getCount()) {
                        item.setCount(ti.getCount());
                     }

                     found = item.getCount() > 0L;
                  }
                  break;
               }
            }

            if (found) {
               if (Inventory.MAX_ADENA / item.getCount() < item.getPrice()) {
                  this.lock();
                  break;
               }

               long _totalPrice = totalPrice + item.getCount() * item.getPrice();
               if (Inventory.MAX_ADENA < _totalPrice || _totalPrice < 0L) {
                  this.lock();
                  break;
               }

               if (ownerInventory.getAdena() >= _totalPrice) {
                  int objectId = item.getObjectId();
                  Item oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
                  if (oldItem == null) {
                     oldItem = playerInventory.getItemByItemId(item.getItemId());
                     if (oldItem == null) {
                        continue;
                     }

                     objectId = oldItem.getObjectId();
                     oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
                     if (oldItem == null) {
                        continue;
                     }
                  }

                  if (oldItem.getId() != item.getItemId()) {
                     PunishmentManager.handleIllegalPlayerAction(player, player + " is cheating with sell items", GeneralConfig.DEFAULT_PUNISH);
                     return false;
                  }

                  if (oldItem.isTradeable()) {
                     Item newItem = playerInventory.transferItem(ItemProcessType.TRANSFER, objectId, item.getCount(), ownerInventory, player, this._owner);
                     if (newItem != null) {
                        this.removeItem(-1, item.getItemId(), item.getCount());
                        ok = true;
                        totalPrice = _totalPrice;
                        if (oldItem.getCount() > 0L && oldItem != newItem) {
                           playerIU.addModifiedItem(oldItem);
                        } else {
                           playerIU.addRemovedItem(oldItem);
                        }

                        if (newItem.getCount() > item.getCount()) {
                           ownerIU.addModifiedItem(newItem);
                        } else {
                           ownerIU.addNewItem(newItem);
                        }

                        SystemMessage var24;
                        if (newItem.isStackable()) {
                           SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_C1);
                           msg.addString(player.getName());
                           msg.addItemName(newItem);
                           msg.addLong(item.getCount());
                           this._owner.sendPacket(msg);
                           var24 = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_PURCHASED_S3_S2_S);
                           var24.addString(this._owner.getName());
                           var24.addItemName(newItem);
                           var24.addLong(item.getCount());
                        } else {
                           SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_C1);
                           msg.addString(player.getName());
                           msg.addItemName(newItem);
                           this._owner.sendPacket(msg);
                           var24 = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_PURCHASED_S2);
                           var24.addString(this._owner.getName());
                           var24.addItemName(newItem);
                        }

                        player.sendPacket(var24);
                     }
                  }
               }
            }
         }

         if (totalPrice > 0L) {
            if (totalPrice > ownerInventory.getAdena()) {
               return false;
            }

            Item adenaItem = ownerInventory.getAdenaInstance();
            ownerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, this._owner, player);
            ownerIU.addItem(adenaItem);
            playerInventory.addAdena(ItemProcessType.SELL, totalPrice, player, this._owner);
            playerIU.addItem(playerInventory.getAdenaInstance());
         }

         if (ok) {
            this._owner.sendInventoryUpdate(ownerIU);
            player.sendInventoryUpdate(playerIU);
         }

         return ok;
      } else {
         return false;
      }
   }
}
