package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.GMAudit;

public class RequestDropItem extends ClientPacket {
   private int _objectId;
   private long _count;
   private int _x;
   private int _y;
   private int _z;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._count = this.readLong();
      this._x = this.readInt();
      this._y = this.readInt();
      this._z = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && !player.isDead()) {
         if (this.getClient().getFloodProtectors().canDropItem()) {
            Item item = player.getInventory().getItemByObjectId(this._objectId);
            if (item != null
               && this._count != 0L
               && player.validateItemManipulation(this._objectId, ItemProcessType.DROP)
               && (GeneralConfig.ALLOW_DISCARDITEM || player.isGM())
               && (item.isDropable() || player.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS)
               && (item.getItemType() != EtcItemType.PET_COLLAR || !player.havePetInvItems())
               && !player.isInsideZone(ZoneId.NO_ITEM_DROP)) {
               if (!item.isQuestItem() || player.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS) {
                  if (this._count > item.getCount()) {
                     player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
                  } else if (PlayerConfig.PLAYER_SPAWN_PROTECTION > 0 && player.isInvul() && !player.isGM()) {
                     player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
                  } else if (this._count < 0L) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        "[RequestDropItem] Character "
                           + player.getName()
                           + " of account "
                           + player.getAccountName()
                           + " tried to drop item with oid "
                           + this._objectId
                           + " but has count < 0!",
                        GeneralConfig.DEFAULT_PUNISH
                     );
                  } else if (!item.isStackable() && this._count > 1L) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        "[RequestDropItem] Character "
                           + player.getName()
                           + " of account "
                           + player.getAccountName()
                           + " tried to drop non-stackable item with oid "
                           + this._objectId
                           + " but has count > 1!",
                        GeneralConfig.DEFAULT_PUNISH
                     );
                  } else if (GeneralConfig.JAIL_DISABLE_TRANSACTION && player.isJailed()) {
                     player.sendMessage("You cannot drop items in Jail.");
                  } else if (!player.getAccessLevel().allowTransaction()) {
                     player.sendMessage("Transactions are disabled for your Access Level.");
                     player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
                  } else if (player.isProcessingTransaction() || player.isInStoreMode()) {
                     player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
                  } else if (player.isFishing()) {
                     player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
                  } else if (!player.isFlying()) {
                     if (player.isCastingNow()) {
                        SkillUseHolder skill = player.getCurrentSkill();
                        if (skill != null) {
                           if (skill.getSkill().getItemConsumeId() == item.getId()
                              && player.getInventory().getInventoryItemCount(item.getId(), -1) - skill.getSkill().getItemConsumeCount() < this._count) {
                              player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
                              return;
                           }

                           if (player.getKnownSkill(skill.getSkillId()) != null) {
                              player.sendMessage("You cannot drop an item while casting " + skill.getSkill().getName() + ".");
                              return;
                           }
                        }
                     }

                     if (player.isCastingSimultaneouslyNow()) {
                        Skill skill = player.getLastSimultaneousSkillCast();
                        if (skill != null) {
                           if (skill.getItemConsumeId() == item.getId()
                              && player.getInventory().getInventoryItemCount(item.getId(), -1) - skill.getItemConsumeCount() < this._count) {
                              player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
                              return;
                           }

                           if (player.getKnownSkill(skill.getId()) != null) {
                              player.sendMessage("You cannot drop an item while casting " + skill.getName() + ".");
                              return;
                           }
                        }
                     }

                     if (3 == item.getTemplate().getType2() && !player.isGM()) {
                        player.sendPacket(SystemMessageId.THAT_ITEM_CANNOT_BE_DISCARDED_OR_EXCHANGED);
                     } else if (!player.isInsideRadius2D(this._x, this._y, 0, 150) || Math.abs(this._z - player.getZ()) > 50) {
                        player.sendPacket(SystemMessageId.THAT_IS_TOO_FAR_FROM_YOU_TO_DISCARD);
                     } else if (!player.getInventory().canManipulateWithItemId(item.getId())) {
                        player.sendMessage("You cannot use this item.");
                     } else {
                        if (item.isEquipped()) {
                           InventoryUpdate iu = new InventoryUpdate();

                           for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot())) {
                              itm.unChargeAllShots();
                              iu.addModifiedItem(itm);
                           }

                           player.sendInventoryUpdate(iu);
                           player.broadcastUserInfo();
                           player.sendItemList(true);
                        }

                        Item dropedItem = player.dropItem(ItemProcessType.DROP, this._objectId, this._count, this._x, this._y, this._z, null, false, false);
                        if (player.isGM()) {
                           String target = player.getTarget() != null ? player.getTarget().getName() : "no-target";
                           GMAudit.logAction(
                              player.getName() + " [" + player.getObjectId() + "]",
                              "Drop",
                              target,
                              "(id: "
                                 + dropedItem.getId()
                                 + " name: "
                                 + dropedItem.getItemName()
                                 + " objId: "
                                 + dropedItem.getObjectId()
                                 + " x: "
                                 + player.getX()
                                 + " y: "
                                 + player.getY()
                                 + " z: "
                                 + player.getZ()
                                 + ")"
                           );
                        }

                        if (dropedItem != null && dropedItem.getId() == 57 && dropedItem.getCount() >= 1000000L) {
                           String msg = "Character ("
                              + player.getName()
                              + ") has dropped ("
                              + dropedItem.getCount()
                              + ")adena at ("
                              + this._x
                              + ","
                              + this._y
                              + ","
                              + this._z
                              + ")";
                           PacketLogger.warning(msg);
                           AdminData.getInstance().broadcastMessageToGMs(msg);
                        }
                     }
                  }
               }
            } else {
               if (item != null && item.isAugmented()) {
                  player.sendPacket(SystemMessageId.THE_AUGMENTED_ITEM_CANNOT_BE_DISCARDED);
               } else {
                  player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
               }
            }
         }
      }
   }
}
