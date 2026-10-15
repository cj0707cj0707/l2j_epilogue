package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class SendWareHouseDepositList extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private List<ItemHolder> _items = null;

   @Override
   protected void readImpl() {
      int size = this.readInt();
      if (size > 0 && size <= PlayerConfig.MAX_ITEM_IN_PACKET && size * 12 == this.remaining()) {
         this._items = new ArrayList<>(size);

         for (int i = 0; i < size; i++) {
            int objId = this.readInt();
            long count = this.readLong();
            if (objId < 1 || count < 1L) {
               this._items = null;
               return;
            }

            this._items.add(new ItemHolder(objId, count));
         }
      }
   }

   @Override
   protected void runImpl() {
      if (this._items != null) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
               player.sendMessage("You are depositing items too fast.");
            } else {
               ItemContainer warehouse = player.getActiveWarehouse();
               if (warehouse != null) {
                  Npc manager = player.getLastFolkNPC();
                  if ((manager == null || !manager.isWarehouse() || !manager.canInteract(player)) && !player.isGM()) {
                     player.sendPacket(SystemMessageId.YOU_FAILED_AT_SENDING_THE_PACKAGE_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_WAREHOUSE);
                  } else {
                     boolean isPrivate = warehouse instanceof PlayerWarehouse;
                     if (!isPrivate && !player.getAccessLevel().allowTransaction()) {
                        player.sendMessage("Transactions are disabled for your Access Level.");
                     } else if (player.getActiveEnchantItemId() != -1) {
                        player.sendPacket(SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
                        player.sendPacket(new EnchantResult(2, 0, 0));
                        player.setActiveEnchantItemId(-1);
                     } else if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE || player.getKarma() <= 0) {
                        long fee = this._items.size() * 30;
                        long currentAdena = player.getAdena();
                        int slots = 0;

                        for (ItemHolder itemHolder : this._items) {
                           Item item = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "deposit");
                           if (item == null) {
                              PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
                              return;
                           }

                           if (item.getId() == 57) {
                              currentAdena -= itemHolder.getCount();
                           }

                           if (!item.isStackable()) {
                              slots = (int)(slots + itemHolder.getCount());
                           } else if (warehouse.getItemByItemId(item.getId()) == null) {
                              slots++;
                           }
                        }

                        if (!warehouse.validateCapacity(slots)) {
                           player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
                        } else if (currentAdena >= fee && player.reduceAdena(ItemProcessType.FEE, fee, manager, false)) {
                           if (player.getActiveTradeList() == null) {
                              InventoryUpdate playerIU = new InventoryUpdate();

                              for (ItemHolder itemHolder : this._items) {
                                 Item oldItem = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "deposit");
                                 if (oldItem == null) {
                                    PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
                                    return;
                                 }

                                 if (oldItem.isDepositable(isPrivate) && oldItem.isAvailable(player, true, isPrivate)) {
                                    Item newItem = player.getInventory()
                                       .transferItem(ItemProcessType.TRANSFER, itemHolder.getId(), itemHolder.getCount(), warehouse, player, manager);
                                    if (newItem == null) {
                                       PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
                                    } else if (oldItem.getCount() > 0L && oldItem != newItem) {
                                       playerIU.addModifiedItem(oldItem);
                                    } else {
                                       playerIU.addRemovedItem(oldItem);
                                    }
                                 }
                              }

                              player.sendInventoryUpdate(playerIU);
                              StatusUpdate su = new StatusUpdate(player);
                              su.addAttribute(14, player.getCurrentLoad());
                              player.sendPacket(su);
                           }
                        } else {
                           player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
