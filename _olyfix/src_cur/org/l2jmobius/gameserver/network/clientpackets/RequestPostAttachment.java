package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import org.l2jmobius.gameserver.network.serverpackets.ExShowReceivedPostList;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPostAttachment extends ClientPacket {
   private int _msgId;

   @Override
   protected void readImpl() {
      this._msgId = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (GeneralConfig.ALLOW_MAIL && GeneralConfig.ALLOW_ATTACHMENTS) {
         Player player = this.getPlayer();
         if (player != null) {
            if (this.getClient().getFloodProtectors().canPerformTransaction()) {
               if (!player.getAccessLevel().allowTransaction()) {
                  player.sendMessage("Transactions are disabled for your Access Level");
               } else if (!player.isInsideZone(ZoneId.PEACE)) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_IN_A_NON_PEACE_ZONE_LOCATION);
               } else if (player.getActiveTradeList() != null) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_DURING_AN_EXCHANGE);
               } else if (player.isEnchanting()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
               } else if (player.isInStoreMode()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
               } else {
                  MailMessage msg = MailManager.getInstance().getMessage(this._msgId);
                  if (msg != null) {
                     if (msg.getReceiverId() != player.getObjectId()) {
                        PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get not own attachment!", GeneralConfig.DEFAULT_PUNISH);
                     } else if (msg.hasAttachments()) {
                        ItemContainer attachments = msg.getAttachments();
                        if (attachments != null) {
                           int weight = 0;
                           int slots = 0;

                           for (Item item : attachments.getItems()) {
                              if (item != null) {
                                 if (item.getOwnerId() != msg.getSenderId()) {
                                    PunishmentManager.handleIllegalPlayerAction(
                                       player, player + " tried to get wrong item (ownerId != senderId) from attachment!", GeneralConfig.DEFAULT_PUNISH
                                    );
                                    return;
                                 }

                                 if (item.getItemLocation() != ItemLocation.MAIL) {
                                    PunishmentManager.handleIllegalPlayerAction(
                                       player, player + " tried to get wrong item (Location != MAIL) from attachment!", GeneralConfig.DEFAULT_PUNISH
                                    );
                                    return;
                                 }

                                 if (item.getLocationSlot() != msg.getId()) {
                                    PunishmentManager.handleIllegalPlayerAction(
                                       player, player + " tried to get items from different attachment!", GeneralConfig.DEFAULT_PUNISH
                                    );
                                    return;
                                 }

                                 weight = (int)(weight + item.getCount() * item.getTemplate().getWeight());
                                 if (!item.isStackable()) {
                                    slots = (int)(slots + item.getCount());
                                 } else if (player.getInventory().getItemByItemId(item.getId()) == null) {
                                    slots++;
                                 }
                              }
                           }

                           if (!player.getInventory().validateCapacity(slots)) {
                              player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
                           } else if (!player.getInventory().validateWeight(weight)) {
                              player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
                           } else {
                              long adena = msg.getReqAdena();
                              if (adena > 0L && !player.reduceAdena(ItemProcessType.FEE, adena, null, true)) {
                                 player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
                              } else {
                                 InventoryUpdate playerIU = new InventoryUpdate();

                                 for (Item itemx : attachments.getItems()) {
                                    if (itemx != null) {
                                       if (itemx.getOwnerId() != msg.getSenderId()) {
                                          PunishmentManager.handleIllegalPlayerAction(
                                             player, player + " tried to get items with owner != sender !", GeneralConfig.DEFAULT_PUNISH
                                          );
                                          return;
                                       }

                                       long count = itemx.getCount();
                                       Item newItem = attachments.transferItem(
                                          ItemProcessType.TRANSFER, itemx.getObjectId(), itemx.getCount(), player.getInventory(), player, null
                                       );
                                       if (newItem == null) {
                                          return;
                                       }

                                       if (newItem.isStackable() && newItem.getCount() > count) {
                                          playerIU.addModifiedItem(newItem);
                                       } else {
                                          playerIU.addNewItem(newItem);
                                       }

                                       SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S2_S1);
                                       sm.addItemName(itemx.getId());
                                       sm.addLong(count);
                                       player.sendPacket(sm);
                                    }
                                 }

                                 player.sendInventoryUpdate(playerIU);
                                 player.sendItemList(false);
                                 msg.removeAttachments();
                                 StatusUpdate su = new StatusUpdate(player);
                                 su.addAttribute(14, player.getCurrentLoad());
                                 player.sendPacket(su);
                                 Player sender = World.getPlayer(msg.getSenderId());
                                 if (adena > 0L) {
                                    if (sender != null) {
                                       sender.addAdena(ItemProcessType.TRANSFER, adena, player, false);
                                       SystemMessage sm = new SystemMessage(SystemMessageId.S2_HAS_MADE_A_PAYMENT_OF_S1_ADENA_PER_YOUR_PAYMENT_REQUEST_MAIL);
                                       sm.addLong(adena);
                                       sm.addString(player.getName());
                                       sender.sendPacket(sm);
                                    } else {
                                       Item paidAdena = ItemManager.createItem(ItemProcessType.FEE, 57, adena, player, null);
                                       paidAdena.setOwnerId(msg.getSenderId());
                                       paidAdena.setItemLocation(ItemLocation.INVENTORY);
                                       paidAdena.updateDatabase(true);
                                       World.removeObject(paidAdena);
                                    }
                                 } else if (sender != null) {
                                    SystemMessage sm = new SystemMessage(SystemMessageId.S1_ACQUIRED_THE_ATTACHED_ITEM_TO_YOUR_MAIL);
                                    sm.addString(player.getName());
                                    sender.sendPacket(sm);
                                 }

                                 player.sendPacket(new ExChangePostState(true, this._msgId, 1));
                                 player.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_RECEIVED);
                                 player.sendPacket(new ExShowReceivedPostList(player.getObjectId()));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
