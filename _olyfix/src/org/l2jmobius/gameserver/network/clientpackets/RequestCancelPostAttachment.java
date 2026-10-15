package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestCancelPostAttachment extends ClientPacket {
   private int _msgId;

   @Override
   protected void readImpl() {
      this._msgId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && GeneralConfig.ALLOW_MAIL && GeneralConfig.ALLOW_ATTACHMENTS) {
         if (this.getClient().getFloodProtectors().canPerformTransaction()) {
            MailMessage msg = MailManager.getInstance().getMessage(this._msgId);
            if (msg != null) {
               if (msg.getSenderId() != player.getObjectId()) {
                  PunishmentManager.handleIllegalPlayerAction(player, player + " tried to cancel not own post!", GeneralConfig.DEFAULT_PUNISH);
               } else if (!player.isInsideZone(ZoneId.PEACE)) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_IN_A_NON_PEACE_ZONE_LOCATION);
               } else if (player.getActiveTradeList() != null) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_DURING_AN_EXCHANGE);
               } else if (player.isEnchanting()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
               } else if (player.isInStoreMode()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
               } else if (!msg.hasAttachments()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_SENT_MAIL_SINCE_THE_RECIPIENT_RECEIVED_IT);
               } else {
                  ItemContainer attachments = msg.getAttachments();
                  if (attachments != null && attachments.getSize() != 0) {
                     int weight = 0;
                     int slots = 0;

                     for (Item item : attachments.getItems()) {
                        if (item != null) {
                           if (item.getOwnerId() != player.getObjectId()) {
                              PunishmentManager.handleIllegalPlayerAction(
                                 player, player + " tried to get not own item from cancelled attachment!", GeneralConfig.DEFAULT_PUNISH
                              );
                              return;
                           }

                           if (item.getItemLocation() != ItemLocation.MAIL) {
                              PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get items not from mail !", GeneralConfig.DEFAULT_PUNISH);
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
                        player.sendPacket(SystemMessageId.YOU_COULD_NOT_CANCEL_RECEIPT_BECAUSE_YOUR_INVENTORY_IS_FULL);
                     } else if (!player.getInventory().validateWeight(weight)) {
                        player.sendPacket(SystemMessageId.YOU_COULD_NOT_CANCEL_RECEIPT_BECAUSE_YOUR_INVENTORY_IS_FULL);
                     } else {
                        InventoryUpdate playerIU = new InventoryUpdate();

                        for (Item itemx : attachments.getItems()) {
                           if (itemx != null) {
                              long count = itemx.getCount();
                              Item newItem = attachments.transferItem(ItemProcessType.TRANSFER, itemx.getObjectId(), count, player.getInventory(), player, null);
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

                        msg.removeAttachments();
                        player.sendInventoryUpdate(playerIU);
                        player.sendItemList(false);
                        StatusUpdate su = new StatusUpdate(player);
                        su.addAttribute(14, player.getCurrentLoad());
                        player.sendPacket(su);
                        Player receiver = World.getPlayer(msg.getReceiverId());
                        if (receiver != null) {
                           SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANCELED_THE_SENT_MAIL);
                           sm.addString(player.getName());
                           receiver.sendPacket(sm);
                           receiver.sendPacket(new ExChangePostState(true, this._msgId, 0));
                        }

                        MailManager.getInstance().deleteMessageInDb(this._msgId);
                        player.sendPacket(new ExChangePostState(false, this._msgId, 0));
                        player.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_CANCELLED);
                     }
                  } else {
                     player.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_SENT_MAIL_SINCE_THE_RECIPIENT_RECEIVED_IT);
                  }
               }
            }
         }
      }
   }
}
