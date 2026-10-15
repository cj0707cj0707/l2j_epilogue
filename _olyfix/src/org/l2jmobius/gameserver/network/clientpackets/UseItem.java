package org.l2jmobius.gameserver.network.clientpackets;

import java.util.concurrent.TimeUnit;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class UseItem extends ClientPacket {
   private int _objectId;
   private boolean _ctrlPressed;
   private int _itemId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._ctrlPressed = this.readInt() != 0;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this.getClient().getFloodProtectors().canUseItem()) {
            if (player.isInsideZone(ZoneId.JAIL)) {
               player.sendMessage("You cannot use items while jailed.");
            } else {
               Item item = player.getInventory().getItemByObjectId(this._objectId);
               if (item != null) {
                  boolean isPetCollar = item.getEtcItem() != null && item.getEtcItem().getItemType() == EtcItemType.PET_COLLAR;
                  if (player.getActiveTradeList() != null && !isPetCollar) {
                     player.sendPacket(SystemMessageId.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING);
                  } else if (player.isInStoreMode() && !isPetCollar) {
                     player.sendPacket(SystemMessageId.YOU_MAY_NOT_USE_ITEMS_IN_A_PRIVATE_STORE_OR_PRIVATE_WORK_SHOP);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  } else if (item.getTemplate().getType2() == 3) {
                     player.sendPacket(SystemMessageId.YOU_CANNOT_USE_QUEST_ITEMS);
                  } else if (!player.isStunned() && !player.isParalyzed() && !player.isSleeping() && !player.isControlBlocked() && !player.isAlikeDead()) {
                     this._itemId = item.getId();
                     if (player.isDead() || !player.getInventory().canManipulateWithItemId(this._itemId)) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
                        sm.addItemName(item);
                        player.sendPacket(sm);
                     } else if (item.isEquipped() || item.getTemplate().checkCondition(player, player, true)) {
                        if (!player.isFishing() || this._itemId >= 6535 && this._itemId <= 6540) {
                           if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0) {
                              SkillHolder[] skills = item.getTemplate().getSkills();
                              if (skills != null) {
                                 for (SkillHolder sHolder : skills) {
                                    Skill skill = sHolder.getSkill();
                                    if (skill != null && skill.hasEffectType(EffectType.TELEPORT)) {
                                       return;
                                    }
                                 }
                              }
                           }

                           int reuseDelay = item.getReuseDelay();
                           int sharedReuseGroup = item.getSharedReuseGroup();
                           if (reuseDelay > 0) {
                              long reuse = player.getItemRemainingReuseTime(item.getObjectId());
                              if (reuse > 0L) {
                                 this.reuseData(player, item, reuse);
                                 this.sendSharedGroupUpdate(player, sharedReuseGroup, reuse, reuseDelay);
                                 return;
                              }

                              long reuseOnGroup = player.getReuseDelayOnGroup(sharedReuseGroup);
                              if (reuseOnGroup > 0L) {
                                 this.reuseData(player, item, reuseOnGroup);
                                 this.sendSharedGroupUpdate(player, sharedReuseGroup, reuseOnGroup, reuseDelay);
                                 return;
                              }
                           }

                           player.onActionRequest();
                           if (item.isEquipable()) {
                              if (player.isCastingNow() || player.isCastingSimultaneouslyNow()) {
                                 player.sendPacket(SystemMessageId.DURING_AN_ATTACK_ONE_CANNOT_CHANGE_WEAPONS);
                                 player.sendPacket(ActionFailed.STATIC_PACKET);
                                 return;
                              }

                              if (player.isCursedWeaponEquipped() && this._itemId == 6408) {
                                 return;
                              }

                              if (FortSiegeManager.getInstance().isCombat(this._itemId)) {
                                 return;
                              }

                              if (player.isCombatFlagEquipped()) {
                                 return;
                              }

                              label200:
                              switch (item.getTemplate().getBodyPart()) {
                                 case LR_HAND:
                                 case L_HAND:
                                 case R_HAND:
                                    if (player.getActiveWeaponItem() != null && player.getActiveWeaponItem().getId() == 9819) {
                                       player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                       return;
                                    }

                                    if (player.isMounted()) {
                                       player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                       return;
                                    }

                                    if (player.isDisarmed()) {
                                       player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                       return;
                                    }

                                    if (player.isCursedWeaponEquipped()) {
                                       return;
                                    }

                                    if (!item.isEquipped() && item.isWeapon() && !player.isGM()) {
                                       Weapon wpn = (Weapon)item.getTemplate();
                                       switch (player.getRace()) {
                                          case KAMAEL:
                                             switch (wpn.getItemType()) {
                                                case NONE:
                                                   player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                                   return;
                                                default:
                                                   break label200;
                                             }
                                          case HUMAN:
                                          case DWARF:
                                          case ELF:
                                          case DARK_ELF:
                                          case ORC:
                                             switch (wpn.getItemType()) {
                                                case RAPIER:
                                                case CROSSBOW:
                                                case ANCIENTSWORD:
                                                   player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                                   return;
                                             }
                                       }
                                    }
                                    break;
                                 case CHEST:
                                 case BACK:
                                 case GLOVES:
                                 case FEET:
                                 case HEAD:
                                 case FULL_ARMOR:
                                 case LEGS:
                                    if (player.getRace() == Race.KAMAEL
                                       && (item.getTemplate().getItemType() == ArmorType.HEAVY || item.getTemplate().getItemType() == ArmorType.MAGIC)) {
                                       player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                       return;
                                    }
                                    break;
                                 case DECO:
                                    if (!item.isEquipped() && player.getInventory().getTalismanSlots() == 0) {
                                       player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                                       return;
                                    }
                              }

                              if (PlayerConfig.OVER_ENCHANT_PROTECTION
                                 && !player.isGM()
                                 && (
                                    item.isWeapon() && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxWeaponEnchant()
                                       || item.getTemplate().getType2() == 2
                                          && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant()
                                       || item.isArmor()
                                          && item.getTemplate().getType2() != 2
                                          && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxArmorEnchant()
                                 )) {
                                 PacketLogger.info("Over-enchanted (+" + item.getEnchantLevel() + ") " + item + " has been removed from " + player);
                                 player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
                                 if (PlayerConfig.OVER_ENCHANT_PUNISHMENT != IllegalActionPunishmentType.NONE) {
                                    player.sendMessage("[Server]: You have over-enchanted items!");
                                    player.sendMessage("[Server]: Respect our server rules.");
                                    player.sendPacket(new ExShowScreenMessage("You have over-enchanted items!", 6000));
                                    PunishmentManager.handleIllegalPlayerAction(
                                       player, player.getName() + " has over-enchanted items.", PlayerConfig.OVER_ENCHANT_PUNISHMENT
                                    );
                                 }

                                 return;
                              }

                              if (!player.isCastingNow() && !player.isCastingSimultaneouslyNow()) {
                                 long currentTime = System.nanoTime();
                                 long attackEndTime = player.getAttackEndTime();
                                 if (attackEndTime > currentTime) {
                                    ThreadPool.schedule(() -> player.useEquippableItem(item, false), TimeUnit.NANOSECONDS.toMillis(attackEndTime - currentTime));
                                 } else {
                                    player.useEquippableItem(item, true);
                                 }
                              } else {
                                 CreatureAI ai = player.getAI();
                                 ai.setNextAction(new NextAction(org.l2jmobius.gameserver.ai.Action.FINISH_CASTING, Intention.CAST, () -> {
                                    ai.setNextAction(null);
                                    player.useEquippableItem(item, !player.isAutoPlaying());
                                 }));
                              }
                           } else {
                              Weapon weaponItem = player.getActiveWeaponItem();
                              if (weaponItem != null
                                 && weaponItem.getItemType() == WeaponType.FISHINGROD
                                 && (
                                    this._itemId >= 6519 && this._itemId <= 6527
                                       || this._itemId >= 7610 && this._itemId <= 7613
                                       || this._itemId >= 7807 && this._itemId <= 7809
                                       || this._itemId >= 8484 && this._itemId <= 8486
                                       || this._itemId >= 8505 && this._itemId <= 8513
                                 )) {
                                 player.getInventory().setPaperdollItem(7, item);
                                 player.broadcastUserInfo();
                                 player.sendItemList(false);
                                 return;
                              }

                              EtcItem etcItem = item.getEtcItem();
                              IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
                              if (handler == null) {
                                 if (etcItem != null && etcItem.getHandlerName() != null) {
                                    PacketLogger.warning("Unmanaged Item handler: " + etcItem.getHandlerName() + " for Item Id: " + this._itemId + "!");
                                 }

                                 return;
                              }

                              if (handler.onItemUse(player, item, this._ctrlPressed) && reuseDelay > 0) {
                                 player.addTimeStampItem(item, reuseDelay);
                                 this.sendSharedGroupUpdate(player, sharedReuseGroup, reuseDelay, reuseDelay);
                              }
                           }
                        } else {
                           player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void reuseData(Player player, Item item, long remainingTime) {
      int hours = (int)(remainingTime / 3600000L);
      int minutes = (int)(remainingTime % 3600000L) / 60000;
      int seconds = (int)(remainingTime / 1000L % 60L);
      SystemMessage sm;
      if (hours > 0) {
         sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
         sm.addItemName(item);
         sm.addInt(hours);
         sm.addInt(minutes);
      } else if (minutes > 0) {
         sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
         sm.addItemName(item);
         sm.addInt(minutes);
      } else {
         sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
         sm.addItemName(item);
      }

      sm.addInt(seconds);
      player.sendPacket(sm);
   }

   private void sendSharedGroupUpdate(Player player, int group, long remaining, int reuse) {
      if (group > 0) {
         player.sendPacket(new ExUseSharedGroupItem(this._itemId, group, remaining, reuse));
      }
   }
}
