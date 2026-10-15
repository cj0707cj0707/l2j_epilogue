package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestPackageSend extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private ItemHolder[] _items = null;
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 12 == this.remaining()) {
         this._items = new ItemHolder[count];

         for (int i = 0; i < count; i++) {
            int objId = this.readInt();
            long cnt = this.readLong();
            if (objId < 1 || cnt < 0L) {
               this._items = null;
               return;
            }

            this._items[i] = new ItemHolder(objId, cnt);
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (this._items != null && player != null && player.getAccountChars().containsKey(this._objectId)) {
         if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are sending freight packages too fast.");
         } else {
            Npc manager = player.getLastFolkNPC();
            if (manager != null && player.isInsideRadius2D(manager, 250)) {
               if (player.getActiveEnchantItemId() != -1) {
                  PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use enchant Exploit!", GeneralConfig.DEFAULT_PUNISH);
               } else if (player.getActiveTradeList() == null) {
                  if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE || player.getKarma() <= 0) {
                     int fee = this._items.length * PlayerConfig.ALT_FREIGHT_PRICE;
                     long currentAdena = player.getAdena();
                     int slots = 0;
                     ItemContainer warehouse = new PlayerFreight(this._objectId);

                     for (ItemHolder i : this._items) {
                        Item item = player.checkItemManipulation(i.getId(), i.getCount(), "freight");
                        if (item == null) {
                           PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
                           warehouse.deleteMe();
                           return;
                        }

                        if (!item.isFreightable()) {
                           warehouse.deleteMe();
                           return;
                        }

                        if (item.getId() == 57) {
                           currentAdena -= i.getCount();
                        } else if (!item.isStackable()) {
                           slots = (int)(slots + i.getCount());
                        } else if (warehouse.getItemByItemId(item.getId()) == null) {
                           slots++;
                        }
                     }

                     if (!warehouse.validateCapacity(slots)) {
                        player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
                        warehouse.deleteMe();
                     } else if (currentAdena >= fee && player.reduceAdena(ItemProcessType.FEE, fee, manager, false)) {
                        InventoryUpdate playerIU = new InventoryUpdate();

                        for (ItemHolder i : this._items) {
                           Item oldItem = player.checkItemManipulation(i.getId(), i.getCount(), "deposit");
                           if (oldItem == null) {
                              PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
                              warehouse.deleteMe();
                              return;
                           }

                           Item newItem = player.getInventory().transferItem(ItemProcessType.TRANSFER, i.getId(), i.getCount(), warehouse, player, null);
                           if (newItem == null) {
                              PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
                           } else if (oldItem.getCount() > 0L && oldItem != newItem) {
                              playerIU.addModifiedItem(oldItem);
                           } else {
                              playerIU.addRemovedItem(oldItem);
                           }
                        }

                        warehouse.deleteMe();
                        player.sendInventoryUpdate(playerIU);
                        StatusUpdate su = new StatusUpdate(player);
                        su.addAttribute(14, player.getCurrentLoad());
                        player.sendPacket(su);
                     } else {
                        player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                        warehouse.deleteMe();
                     }
                  }
               }
            }
         }
      }
   }
}
