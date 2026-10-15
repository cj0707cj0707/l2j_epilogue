package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ClanWarehouse;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class SendWareHouseWithDrawList extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private ItemHolder[] _items = null;

   @Override
   protected void readImpl() {
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
      if (this._items != null) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
               player.sendMessage("You are withdrawing items too fast.");
            } else {
               ItemContainer warehouse = player.getActiveWarehouse();
               if (warehouse != null) {
                  Npc manager = player.getLastFolkNPC();
                  if ((manager == null || !manager.isWarehouse() || !manager.canInteract(player)) && !player.isGM()) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.YOU_FAILED_AT_SENDING_THE_PACKAGE_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_WAREHOUSE
                     );
                  } else if (!(warehouse instanceof PlayerWarehouse) && !player.getAccessLevel().allowTransaction()) {
                     player.sendMessage("Transactions are disabled for your Access Level.");
                  } else if (player.getActiveEnchantItemId() != -1) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
                     player.sendPacket(new EnchantResult(2, 0, 0));
                     player.setActiveEnchantItemId(-1);
                  } else if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE || player.getKarma() <= 0) {
                     if (PlayerConfig.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH) {
                        if (warehouse instanceof ClanWarehouse && !player.hasAccess(ClanAccess.ACCESS_WAREHOUSE)) {
                           return;
                        }
                     } else if (warehouse instanceof ClanWarehouse && !player.isClanLeader()) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.ITEMS_LEFT_AT_THE_CLAN_HALL_WAREHOUSE_CAN_ONLY_BE_RETRIEVED_BY_THE_CLAN_LEADER_DO_YOU_WANT_TO_CONTINUE
                        );
                        return;
                     }

                     int weight = 0;
                     int slots = 0;

                     for (ItemHolder i : this._items) {
                        Item item = warehouse.getItemByObjectId(i.getId());
                        if (item == null || item.getCount() < i.getCount()) {
                           PunishmentManager.handleIllegalPlayerAction(
                              player,
                              "Warning!! Character "
                                 + player.getName()
                                 + " of account "
                                 + player.getAccountName()
                                 + " tried to withdraw non-existent item from warehouse.",
                              GeneralConfig.DEFAULT_PUNISH
                           );
                           return;
                        }

                        weight = (int)(weight + i.getCount() * item.getTemplate().getWeight());
                        if (!item.isStackable()) {
                           slots = (int)(slots + i.getCount());
                        } else if (player.getInventory().getItemByItemId(item.getId()) == null) {
                           slots++;
                        }
                     }

                     if (!player.getInventory().validateCapacity(slots)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
                     } else if (!player.getInventory().validateWeight(weight)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                     } else {
                        InventoryUpdate playerIU = new InventoryUpdate();

                        for (ItemHolder i : this._items) {
                           Item oldItem = warehouse.getItemByObjectId(i.getId());
                           if (oldItem == null || oldItem.getCount() < i.getCount()) {
                              org.l2jmobius.gameserver.network.PacketLogger.warning(
                                 "Error withdrawing a warehouse object for char " + player.getName() + " (olditem == null)"
                              );
                              return;
                           }

                           Item newItem = warehouse.transferItem(ItemProcessType.TRANSFER, i.getId(), i.getCount(), player.getInventory(), player, manager);
                           if (newItem == null) {
                              org.l2jmobius.gameserver.network.PacketLogger.warning(
                                 "Error withdrawing a warehouse object for char " + player.getName() + " (newitem == null)"
                              );
                              return;
                           }

                           if (newItem.getCount() > i.getCount()) {
                              playerIU.addModifiedItem(newItem);
                           } else {
                              playerIU.addNewItem(newItem);
                           }
                        }

                        player.sendInventoryUpdate(playerIU);
                        StatusUpdate su = new StatusUpdate(player);
                        su.addAttribute(14, player.getCurrentLoad());
                        player.sendPacket(su);
                     }
                  }
               }
            }
         }
      }
   }
}
