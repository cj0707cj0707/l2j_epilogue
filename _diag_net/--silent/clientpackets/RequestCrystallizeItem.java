package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestCrystallizeItem extends ClientPacket {
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
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are crystallizing too fast.");
         } else if (this._count < 1L) {
            PunishmentManager.handleIllegalPlayerAction(
               player, "[RequestCrystallizeItem] count <= 0! ban! oid: " + this._objectId + " owner: " + player.getName(), GeneralConfig.DEFAULT_PUNISH
            );
         } else if (!player.isInStoreMode() && !player.isInCrystallize()) {
            int skillLevel = player.getSkillLevel(CommonSkill.CRYSTALLIZE.getId());
            if (skillLevel <= 0) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_MAY_NOT_CRYSTALLIZE_THIS_ITEM_YOUR_CRYSTALLIZATION_SKILL_LEVEL_IS_TOO_LOW);
               player.sendPacket(ActionFailed.STATIC_PACKET);
               if (player.getRace() != Race.DWARF && player.getPlayerClass().getId() != 117 && player.getPlayerClass().getId() != 55) {
                  org.l2jmobius.gameserver.network.PacketLogger.info(
                     "Player " + this.getClient() + " used crystalize with classid: " + player.getPlayerClass().getId()
                  );
               }
            } else {
               PlayerInventory inventory = player.getInventory();
               if (inventory != null) {
                  Item item = inventory.getItemByObjectId(this._objectId);
                  if (item == null || item.isHeroItem() || !PlayerConfig.ALT_ALLOW_AUGMENT_DESTROY && item.isAugmented()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (this._count > item.getCount()) {
                     this._count = player.getInventory().getItemByObjectId(this._objectId).getCount();
                  }
               }

               Item itemToRemove = player.getInventory().getItemByObjectId(this._objectId);
               if (itemToRemove != null && !itemToRemove.isShadowItem() && !itemToRemove.isTimeLimitedItem()) {
                  if (!itemToRemove.getTemplate().isCrystallizable()
                     || itemToRemove.getTemplate().getCrystalCount() <= 0
                     || itemToRemove.getTemplate().getCrystalType() == CrystalType.NONE) {
                     org.l2jmobius.gameserver.network.PacketLogger.warning(
                        player.getName() + " (" + player.getObjectId() + ") tried to crystallize " + itemToRemove.getTemplate().getId()
                     );
                  } else if (!player.getInventory().canManipulateWithItemId(itemToRemove.getId())) {
                     player.sendMessage("You cannot use this item.");
                  } else {
                     boolean canCrystallize = true;
                     switch (itemToRemove.getTemplate().getCrystalTypePlus()) {
                        case C:
                           if (skillLevel <= 1) {
                              canCrystallize = false;
                           }
                           break;
                        case B:
                           if (skillLevel <= 2) {
                              canCrystallize = false;
                           }
                           break;
                        case A:
                           if (skillLevel <= 3) {
                              canCrystallize = false;
                           }
                           break;
                        case S:
                           if (skillLevel <= 4) {
                              canCrystallize = false;
                           }
                     }

                     if (!canCrystallize) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_MAY_NOT_CRYSTALLIZE_THIS_ITEM_YOUR_CRYSTALLIZATION_SKILL_LEVEL_IS_TOO_LOW
                        );
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                     } else {
                        player.setInCrystallize(true);
                        if (itemToRemove.isEquipped()) {
                           InventoryUpdate iu = new InventoryUpdate();

                           for (Item itemx : player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot())) {
                              iu.addModifiedItem(itemx);
                           }

                           player.sendPacket(iu);
                           SystemMessage sm;
                           if (itemToRemove.isEnchanted()) {
                              sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                              sm.addInt(itemToRemove.getEnchantLevel());
                              sm.addItemName(itemToRemove);
                           } else {
                              sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_BEEN_DISARMED);
                              sm.addItemName(itemToRemove);
                           }

                           player.sendPacket(sm);
                        }

                        Item removedItem = player.getInventory().destroyItem(ItemProcessType.DESTROY, this._objectId, this._count, player, null);
                        InventoryUpdate iu = new InventoryUpdate();
                        iu.addRemovedItem(removedItem);
                        player.sendPacket(iu);
                        int crystalId = itemToRemove.getTemplate().getCrystalItemId();
                        int crystalAmount = itemToRemove.getCrystalCount();
                        Item createditem = player.getInventory().addItem(ItemProcessType.COMPENSATE, crystalId, crystalAmount, player, player);
                        SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_BEEN_CRYSTALLIZED);
                        sm.addItemName(removedItem);
                        player.sendPacket(sm);
                        sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                        sm.addItemName(createditem);
                        sm.addLong(crystalAmount);
                        player.sendPacket(sm);
                        player.broadcastUserInfo();
                        World.removeObject(removedItem);
                        player.setInCrystallize(false);
                     }
                  }
               }
            }
         } else {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM
            );
         }
      }
   }
}
