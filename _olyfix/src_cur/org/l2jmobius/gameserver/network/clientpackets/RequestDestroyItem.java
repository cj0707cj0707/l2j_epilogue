package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestDestroyItem extends ClientPacket {
   private int _objectId;
   private long _count;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._count = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._count < 1L) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_DESTROY_IT_BECAUSE_THE_NUMBER_IS_INCORRECT);
            if (this._count < 0L) {
               PunishmentManager.handleIllegalPlayerAction(
                  player,
                  "[RequestDestroyItem] Character "
                     + player.getName()
                     + " of account "
                     + player.getAccountName()
                     + " tried to destroy item with oid "
                     + this._objectId
                     + " but has count < 0!",
                  GeneralConfig.DEFAULT_PUNISH
               );
            }
         } else if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are destroying items too fast.");
         } else {
            long count = this._count;
            if (!player.isProcessingTransaction() && !player.isInStoreMode()) {
               Item itemToRemove = player.getInventory().getItemByObjectId(this._objectId);
               if (itemToRemove == null) {
                  player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
               } else if (player.isCastingNow()
                  && player.getCurrentSkill() != null
                  && player.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getId()) {
                  player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
               } else if (player.isCastingSimultaneouslyNow()
                  && player.getLastSimultaneousSkillCast() != null
                  && player.getLastSimultaneousSkillCast().getItemConsumeId() == itemToRemove.getId()) {
                  player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
               } else {
                  int itemId = itemToRemove.getId();
                  if (GeneralConfig.DESTROY_ALL_ITEMS
                     || (player.isGM() || itemToRemove.isDestroyable()) && !CursedWeaponsManager.getInstance().isCursed(itemId)) {
                     if (!itemToRemove.isStackable() && count > 1L) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "[RequestDestroyItem] Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to destroy a non-stackable item with oid "
                              + this._objectId
                              + " but has count > 1!",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                     } else if (!player.getInventory().canManipulateWithItemId(itemToRemove.getId())) {
                        player.sendMessage("You cannot use this item.");
                     } else {
                        if (this._count > itemToRemove.getCount()) {
                           count = itemToRemove.getCount();
                        }

                        if (itemToRemove.getTemplate().isPetItem()) {
                           Summon summon = player.getSummon();
                           if (summon != null && summon.getControlObjectId() == this._objectId
                              || player.isMounted() && player.getMountObjectID() == this._objectId) {
                              player.sendPacket(SystemMessageId.YOUR_PET_HAS_BEEN_SUMMONED_AND_MAY_NOT_BE_DESTROYED);
                              return;
                           }

                           try (
                              Connection con = DatabaseFactory.getConnection();
                              PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
                           ) {
                              statement.setInt(1, this._objectId);
                              statement.execute();
                           } catch (Exception var15) {
                              PacketLogger.warning("Could not delete pet objectid: " + var15.getMessage());
                           }
                        }

                        if (itemToRemove.isEquipped()) {
                           if (itemToRemove.isEnchanted()) {
                              SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                              sm.addInt(itemToRemove.getEnchantLevel());
                              sm.addItemName(itemToRemove);
                              player.sendPacket(sm);
                           } else {
                              SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
                              sm.addItemName(itemToRemove);
                              player.sendPacket(sm);
                           }

                           InventoryUpdate iu = new InventoryUpdate();

                           for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot())) {
                              iu.addModifiedItem(itm);
                           }

                           player.sendPacket(iu);
                        }

                        Item removedItem = player.getInventory().destroyItem(ItemProcessType.DESTROY, itemToRemove, count, player, null);
                        if (removedItem != null) {
                           InventoryUpdate iu = new InventoryUpdate();
                           if (removedItem.getCount() == 0L) {
                              iu.addRemovedItem(removedItem);
                           } else {
                              iu.addModifiedItem(removedItem);
                           }

                           player.sendPacket(iu);
                           StatusUpdate su = new StatusUpdate(player);
                           su.addAttribute(14, player.getCurrentLoad());
                           player.sendPacket(su);
                           SystemMessage sm;
                           if (count > 1L) {
                              sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                              sm.addItemName(removedItem);
                              sm.addLong(count);
                           } else {
                              sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                              sm.addItemName(removedItem);
                           }

                           player.sendPacket(sm);
                        }
                     }
                  } else {
                     if (itemToRemove.isHeroItem()) {
                        player.sendPacket(SystemMessageId.HERO_WEAPONS_CANNOT_BE_DESTROYED);
                     } else {
                        player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
                     }
                  }
               }
            } else {
               player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
            }
         }
      }
   }
}
