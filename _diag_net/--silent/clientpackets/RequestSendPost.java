package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostSent;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestSendPost extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private static final int MAX_RECV_LENGTH = 16;
   private static final int MAX_SUBJ_LENGTH = 128;
   private static final int MAX_TEXT_LENGTH = 512;
   private static final int MAX_ATTACHMENTS = 8;
   private static final int INBOX_SIZE = 240;
   private static final int OUTBOX_SIZE = 240;
   private static final int MESSAGE_FEE = 100;
   private static final int MESSAGE_FEE_PER_SLOT = 1000;
   private String _receiver;
   private boolean _isCod;
   private String _subject;
   private String _text;
   private RequestSendPost.AttachmentItem[] _items = null;
   private long _reqAdena;

   @Override
   protected void readImpl() {
      this._receiver = this.readString();
      this._isCod = this.readInt() != 0;
      this._subject = this.readString();
      this._text = this.readString();
      int attachCount = this.readInt();
      if (attachCount >= 0 && attachCount <= PlayerConfig.MAX_ITEM_IN_PACKET && attachCount * 12 + 8 == this.remaining()) {
         if (attachCount > 0) {
            this._items = new RequestSendPost.AttachmentItem[attachCount];

            for (int i = 0; i < attachCount; i++) {
               int objectId = this.readInt();
               long count = this.readLong();
               if (objectId < 1 || count < 1L) {
                  this._items = null;
                  return;
               }

               this._items[i] = new RequestSendPost.AttachmentItem(objectId, count);
            }
         }

         this._reqAdena = this.readLong();
         if (this._reqAdena < 0L) {
            this._items = null;
         }
      }
   }

   @Override
   protected void runImpl() {
      if (GeneralConfig.ALLOW_MAIL) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!GeneralConfig.ALLOW_ATTACHMENTS) {
               this._items = null;
               this._isCod = false;
               this._reqAdena = 0L;
            }

            if (!player.getAccessLevel().allowTransaction()) {
               player.sendMessage("Transactions are disabled for your Access Level.");
            } else if (!player.isInsideZone(ZoneId.PEACE) && this._items != null) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_IN_A_NON_PEACE_ZONE_LOCATION);
            } else if (player.getActiveTradeList() != null) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_DURING_AN_EXCHANGE);
            } else if (player.isInventoryDisabled()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_DURING_AN_EXCHANGE);
            } else if (player.isEnchanting()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
            } else if (player.isInStoreMode()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_BECAUSE_THE_PRIVATE_SHOP_OR_WORKSHOP_IS_IN_PROGRESS);
            } else if (this._receiver.length() > 16) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_ALLOWED_LENGTH_FOR_RECIPIENT_EXCEEDED);
            } else if (this._subject.length() > 128) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
            } else if (this._text.length() > 512) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
            } else if (this._items != null && this._items.length > 8) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ITEM_SELECTION_IS_POSSIBLE_UP_TO_8);
            } else if (this._reqAdena >= 0L && this._reqAdena <= Inventory.MAX_ADENA) {
               if (this._isCod) {
                  if (this._reqAdena == 0L) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.WHEN_NOT_ENTERING_THE_AMOUNT_FOR_THE_PAYMENT_REQUEST_YOU_CANNOT_SEND_ANY_MAIL
                     );
                     return;
                  }

                  if (this._items == null || this._items.length == 0) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.IT_S_A_PAYMENT_REQUEST_TRANSACTION_PLEASE_ATTACH_THE_ITEM);
                     return;
                  }
               }

               int receiverId = CharInfoTable.getInstance().getIdByName(this._receiver);
               if (receiverId <= 0) {
                  player.sendPacket(
                     org.l2jmobius.gameserver.network.SystemMessageId.WHEN_THE_RECIPIENT_DOESN_T_EXIST_OR_THE_CHARACTER_HAS_BEEN_DELETED_SENDING_MAIL_IS_NOT_POSSIBLE
                  );
               } else if (receiverId == player.getObjectId()) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_SEND_A_MAIL_TO_YOURSELF);
               } else {
                  int level = CharInfoTable.getInstance().getAccessLevelById(receiverId);
                  AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
                  if (accessLevel.isGm() && !player.getAccessLevel().isGm()) {
                     SystemMessage sm = new SystemMessage(
                        org.l2jmobius.gameserver.network.SystemMessageId.YOUR_MESSAGE_TO_C1_DID_NOT_REACH_ITS_RECIPIENT_YOU_CANNOT_SEND_MAIL_TO_THE_GM_STAFF
                     );
                     sm.addString(this._receiver);
                     player.sendPacket(sm);
                  } else if (player.isJailed() && (GeneralConfig.JAIL_DISABLE_TRANSACTION && this._items != null || GeneralConfig.JAIL_DISABLE_CHAT)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_IN_A_NON_PEACE_ZONE_LOCATION);
                  } else if (BlockList.isInBlockList(receiverId, player.getObjectId())) {
                     SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_HAS_BLOCKED_YOU_YOU_CANNOT_SEND_MAIL_TO_C1);
                     sm.addString(this._receiver);
                     player.sendPacket(sm);
                  } else if (MailManager.getInstance().getOutboxSize(player.getObjectId()) >= 240) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
                  } else if (MailManager.getInstance().getInboxSize(receiverId) >= 240) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
                  } else if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canSendMail()) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.THE_PREVIOUS_MAIL_WAS_FORWARDED_LESS_THAN_1_MINUTE_AGO_AND_THIS_CANNOT_BE_FORWARDED
                     );
                  } else {
                     MailMessage msg = new MailMessage(player.getObjectId(), receiverId, this._isCod, this._subject, this._text, this._reqAdena);
                     if (this.removeItems(player, msg)) {
                        player.setMultiSell(null);
                        MailManager.getInstance().sendMessage(msg);
                        player.sendPacket(ExNoticePostSent.valueOf(true));
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.MAIL_SUCCESSFULLY_SENT);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean removeItems(Player player, MailMessage msg) {
      long currentAdena = player.getAdena();
      long fee = 100L;
      if (this._items != null) {
         for (RequestSendPost.AttachmentItem i : this._items) {
            Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
            if (item == null || !item.isTradeable() || item.isEquipped()) {
               player.sendPacket(
                  org.l2jmobius.gameserver.network.SystemMessageId.THE_ITEM_THAT_YOU_RE_TRYING_TO_SEND_CANNOT_BE_FORWARDED_BECAUSE_IT_ISN_T_PROPER
               );
               return false;
            }

            fee += 1000L;
            if (item.getId() == 57) {
               currentAdena -= i.getCount();
            }
         }
      }

      if (currentAdena < fee || !player.reduceAdena(ItemProcessType.FEE, fee, null, false)) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_FORWARD_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
         return false;
      } else if (this._items == null) {
         return true;
      } else {
         Mail attachments = msg.createAttachments();
         if (attachments == null) {
            return false;
         } else {
            StringBuilder recv = new StringBuilder(32);
            recv.append(msg.getReceiverName() + "[" + msg.getReceiverId() + "]");
            String receiver = recv.toString();
            InventoryUpdate playerIU = new InventoryUpdate();

            for (RequestSendPost.AttachmentItem i : this._items) {
               Item oldItem = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
               if (oldItem == null || !oldItem.isTradeable() || oldItem.isEquipped()) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning("Error adding attachment for char " + player.getName() + " (olditem == null)");
                  return false;
               }

               Item newItem = player.getInventory().transferItem(ItemProcessType.TRANSFER, i.getObjectId(), i.getCount(), attachments, player, receiver);
               if (newItem == null) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning("Error adding attachment for char " + player.getName() + " (newitem == null)");
               } else {
                  newItem.setItemLocation(newItem.getItemLocation(), msg.getId());
                  if (oldItem.getCount() > 0L && oldItem != newItem) {
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
            return true;
         }
      }
   }

   private static class AttachmentItem {
      private final int _objectId;
      private final long _count;

      public AttachmentItem(int id, long num) {
         this._objectId = id;
         this._count = num;
      }

      public int getObjectId() {
         return this._objectId;
      }

      public long getCount() {
         return this._count;
      }
   }
}
