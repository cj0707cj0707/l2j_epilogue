package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantResultType;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestEnchantItem extends ClientPacket {
   protected static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.items");
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && this._objectId != 0) {
         if (!player.isOnline() || this.getClient().isDetached()) {
            player.setActiveEnchantItemId(-1);
         } else if (!player.isProcessingTransaction() && !player.isInStoreMode()) {
            Item item = player.getInventory().getItemByObjectId(this._objectId);
            Item scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
            if (item != null && scroll != null) {
               EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
               if (scrollTemplate != null) {
                  Item support = player.getInventory().getItemByObjectId(player.getActiveEnchantSupportItemId());
                  EnchantSupportItem supportTemplate = null;
                  if (support != null) {
                     supportTemplate = EnchantItemData.getInstance().getSupportItem(support);
                     if (supportTemplate == null) {
                        player.setActiveEnchantItemId(-1);
                        return;
                     }
                  }

                  if (scrollTemplate.isValid(item, supportTemplate)
                     && (!PlayerConfig.DISABLE_OVER_ENCHANTING || item.getEnchantLevel() != scrollTemplate.getMaxEnchantLevel())) {
                     if (player.getActiveEnchantTimestamp() != 0L && System.currentTimeMillis() - player.getActiveEnchantTimestamp() >= 2000L) {
                        Item destroyedScrollItem = player.getInventory().destroyItem(ItemProcessType.FEE, scroll.getObjectId(), 1L, player, item);
                        if (destroyedScrollItem == null) {
                           player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
                           PunishmentManager.handleIllegalPlayerAction(
                              player, player + " tried to enchant with a scroll he doesn't have", GeneralConfig.DEFAULT_PUNISH
                           );
                           player.setActiveEnchantItemId(-1);
                           player.sendPacket(new EnchantResult(2, 0, 0));
                        } else {
                           InventoryUpdate iu = new InventoryUpdate();
                           if (destroyedScrollItem.getCount() > 0L) {
                              iu.addModifiedItem(destroyedScrollItem);
                           } else {
                              iu.addRemovedItem(destroyedScrollItem);
                           }

                           if (support != null) {
                              Item destroyedSupportItem = player.getInventory().destroyItem(ItemProcessType.FEE, support.getObjectId(), 1L, player, item);
                              if (destroyedSupportItem == null) {
                                 player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
                                 PunishmentManager.handleIllegalPlayerAction(
                                    player, player + " tried to enchant with a support item he doesn't have", GeneralConfig.DEFAULT_PUNISH
                                 );
                                 player.setActiveEnchantItemId(-1);
                                 player.sendPacket(new EnchantResult(2, 0, 0));
                                 return;
                              }

                              if (destroyedSupportItem.getCount() > 0L) {
                                 iu.addModifiedItem(destroyedSupportItem);
                              } else {
                                 iu.addRemovedItem(destroyedSupportItem);
                              }
                           }

                           synchronized (item) {
                              if (item.getOwnerId() == player.getObjectId() && item.isEnchantable()) {
                                 EnchantResultType resultType = scrollTemplate.calculateSuccess(player, item, supportTemplate);
                                 switch (resultType) {
                                    case ERROR:
                                       player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
                                       player.setActiveEnchantItemId(-1);
                                       player.sendPacket(new EnchantResult(2, 0, 0));
                                       break;
                                    case SUCCESS:
                                       Skill enchant4Skill = null;
                                       ItemTemplate it = item.getTemplate();
                                       if (scrollTemplate.getChance(player, item) > 0.0) {
                                          item.setEnchantLevel(item.getEnchantLevel() + 1);
                                          iu.addModifiedItem(item);
                                          item.updateDatabase();
                                       }

                                       player.sendPacket(new EnchantResult(0, 0, 0));
                                       if (GeneralConfig.LOG_ITEM_ENCHANTS) {
                                          StringBuilder sb = new StringBuilder();
                                          if (item.isEnchanted()) {
                                             if (support == null) {
                                                LOGGER_ENCHANT.info(
                                                   sb.append("Success, Character:")
                                                      .append(player.getName())
                                                      .append(" [")
                                                      .append(player.getObjectId())
                                                      .append("] Account:")
                                                      .append(player.getAccountName())
                                                      .append(" IP:")
                                                      .append(player.getIPAddress())
                                                      .append(", +")
                                                      .append(item.getEnchantLevel())
                                                      .append(' ')
                                                      .append(item.getName())
                                                      .append('(')
                                                      .append(item.getCount())
                                                      .append(") [")
                                                      .append(item.getObjectId())
                                                      .append("], ")
                                                      .append(scroll.getName())
                                                      .append('(')
                                                      .append(scroll.getCount())
                                                      .append(") [")
                                                      .append(scroll.getObjectId())
                                                      .append(']')
                                                      .toString()
                                                );
                                             } else {
                                                LOGGER_ENCHANT.info(
                                                   sb.append("Success, Character:")
                                                      .append(player.getName())
                                                      .append(" [")
                                                      .append(player.getObjectId())
                                                      .append("] Account:")
                                                      .append(player.getAccountName())
                                                      .append(" IP:")
                                                      .append(player.getIPAddress())
                                                      .append(", +")
                                                      .append(item.getEnchantLevel())
                                                      .append(' ')
                                                      .append(item.getName())
                                                      .append('(')
                                                      .append(item.getCount())
                                                      .append(") [")
                                                      .append(item.getObjectId())
                                                      .append("], ")
                                                      .append(scroll.getName())
                                                      .append('(')
                                                      .append(scroll.getCount())
                                                      .append(") [")
                                                      .append(scroll.getObjectId())
                                                      .append("], ")
                                                      .append(support.getName())
                                                      .append('(')
                                                      .append(support.getCount())
                                                      .append(") [")
                                                      .append(support.getObjectId())
                                                      .append(']')
                                                      .toString()
                                                );
                                             }
                                          } else if (support == null) {
                                             LOGGER_ENCHANT.info(
                                                sb.append("Success, Character:")
                                                   .append(player.getName())
                                                   .append(" [")
                                                   .append(player.getObjectId())
                                                   .append("] Account:")
                                                   .append(player.getAccountName())
                                                   .append(" IP:")
                                                   .append(player.getIPAddress())
                                                   .append(", ")
                                                   .append(item.getName())
                                                   .append('(')
                                                   .append(item.getCount())
                                                   .append(") [")
                                                   .append(item.getObjectId())
                                                   .append("], ")
                                                   .append(scroll.getName())
                                                   .append('(')
                                                   .append(scroll.getCount())
                                                   .append(") [")
                                                   .append(scroll.getObjectId())
                                                   .append(']')
                                                   .toString()
                                             );
                                          } else {
                                             LOGGER_ENCHANT.info(
                                                sb.append("Success, Character:")
                                                   .append(player.getName())
                                                   .append(" [")
                                                   .append(player.getObjectId())
                                                   .append("] Account:")
                                                   .append(player.getAccountName())
                                                   .append(" IP:")
                                                   .append(player.getIPAddress())
                                                   .append(", ")
                                                   .append(item.getName())
                                                   .append('(')
                                                   .append(item.getCount())
                                                   .append(") [")
                                                   .append(item.getObjectId())
                                                   .append("], ")
                                                   .append(scroll.getName())
                                                   .append('(')
                                                   .append(scroll.getCount())
                                                   .append(") [")
                                                   .append(scroll.getObjectId())
                                                   .append("], ")
                                                   .append(support.getName())
                                                   .append('(')
                                                   .append(support.getCount())
                                                   .append(") [")
                                                   .append(support.getObjectId())
                                                   .append(']')
                                                   .toString()
                                             );
                                          }
                                       }

                                       int minEnchantAnnounce = item.isArmor() ? 6 : 7;
                                       int maxEnchantAnnounce = item.isArmor() ? 0 : 15;
                                       if (item.getEnchantLevel() == minEnchantAnnounce || item.getEnchantLevel() == maxEnchantAnnounce) {
                                          SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_SUCCESSFULY_ENCHANTED_A_S2_S3);
                                          sm.addString(player.getName());
                                          sm.addInt(item.getEnchantLevel());
                                          sm.addItemName(item);
                                          player.broadcastPacket(sm);
                                          Skill skill = CommonSkill.FIREWORK.getSkill();
                                          if (skill != null) {
                                             player.broadcastSkillPacket(
                                                new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()),
                                                player
                                             );
                                          }
                                       }

                                       if (item.isArmor() && item.getEnchantLevel() == 4 && item.isEquipped()) {
                                          enchant4Skill = it.getEnchant4Skill();
                                          if (enchant4Skill != null) {
                                             player.addSkill(enchant4Skill, false);
                                             player.sendSkillList();
                                          }
                                       }

                                       player.broadcastUserInfo();
                                       break;
                                    case FAILURE:
                                       if (scrollTemplate.isSafe()) {
                                          player.sendPacket(
                                             SystemMessageId.ENCHANT_FAILED_THE_ENCHANT_SKILL_FOR_THE_CORRESPONDING_ITEM_WILL_BE_EXACTLY_RETAINED
                                          );
                                          player.sendPacket(new EnchantResult(5, 0, 0));
                                          if (GeneralConfig.LOG_ITEM_ENCHANTS) {
                                             StringBuilder sb = new StringBuilder();
                                             if (item.isEnchanted()) {
                                                if (support == null) {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Safe Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", +")
                                                         .append(item.getEnchantLevel())
                                                         .append(' ')
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                } else {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Safe Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", +")
                                                         .append(item.getEnchantLevel())
                                                         .append(' ')
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append("], ")
                                                         .append(support.getName())
                                                         .append('(')
                                                         .append(support.getCount())
                                                         .append(") [")
                                                         .append(support.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                }
                                             } else if (support == null) {
                                                LOGGER_ENCHANT.info(
                                                   sb.append("Safe Fail, Character:")
                                                      .append(player.getName())
                                                      .append(" [")
                                                      .append(player.getObjectId())
                                                      .append("] Account:")
                                                      .append(player.getAccountName())
                                                      .append(" IP:")
                                                      .append(player.getIPAddress())
                                                      .append(", ")
                                                      .append(item.getName())
                                                      .append('(')
                                                      .append(item.getCount())
                                                      .append(") [")
                                                      .append(item.getObjectId())
                                                      .append("], ")
                                                      .append(scroll.getName())
                                                      .append('(')
                                                      .append(scroll.getCount())
                                                      .append(") [")
                                                      .append(scroll.getObjectId())
                                                      .append(']')
                                                      .toString()
                                                );
                                             } else {
                                                LOGGER_ENCHANT.info(
                                                   sb.append("Safe Fail, Character:")
                                                      .append(player.getName())
                                                      .append(" [")
                                                      .append(player.getObjectId())
                                                      .append("] Account:")
                                                      .append(player.getAccountName())
                                                      .append(" IP:")
                                                      .append(player.getIPAddress())
                                                      .append(", ")
                                                      .append(item.getName())
                                                      .append('(')
                                                      .append(item.getCount())
                                                      .append(") [")
                                                      .append(item.getObjectId())
                                                      .append("], ")
                                                      .append(scroll.getName())
                                                      .append('(')
                                                      .append(scroll.getCount())
                                                      .append(") [")
                                                      .append(scroll.getObjectId())
                                                      .append("], ")
                                                      .append(support.getName())
                                                      .append('(')
                                                      .append(support.getCount())
                                                      .append(") [")
                                                      .append(support.getObjectId())
                                                      .append(']')
                                                      .toString()
                                                );
                                             }
                                          }
                                       } else {
                                          if (item.isEquipped()) {
                                             if (item.isEnchanted()) {
                                                SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                                                sm.addInt(item.getEnchantLevel());
                                                sm.addItemName(item);
                                                player.sendPacket(sm);
                                             } else {
                                                SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
                                                sm.addItemName(item);
                                                player.sendPacket(sm);
                                             }

                                             for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot())) {
                                                iu.addModifiedItem(itm);
                                             }
                                          }

                                          if (scrollTemplate.isBlessed()) {
                                             player.sendPacket(SystemMessageId.FAILED_IN_BLESSED_ENCHANT_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
                                             item.setEnchantLevel(0);
                                             iu.addModifiedItem(item);
                                             item.updateDatabase();
                                             player.sendPacket(new EnchantResult(3, 0, 0));
                                             if (GeneralConfig.LOG_ITEM_ENCHANTS) {
                                                StringBuilder sb = new StringBuilder();
                                                if (item.isEnchanted()) {
                                                   if (support == null) {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Blessed Fail, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", +")
                                                            .append(item.getEnchantLevel())
                                                            .append(' ')
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   } else {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Blessed Fail, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", +")
                                                            .append(item.getEnchantLevel())
                                                            .append(' ')
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append("], ")
                                                            .append(support.getName())
                                                            .append('(')
                                                            .append(support.getCount())
                                                            .append(") [")
                                                            .append(support.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   }
                                                } else if (support == null) {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Blessed Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", ")
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                } else {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Blessed Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", ")
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append("], ")
                                                         .append(support.getName())
                                                         .append('(')
                                                         .append(support.getCount())
                                                         .append(") [")
                                                         .append(support.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                }
                                             }
                                          } else {
                                             Item destroyedItem = player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
                                             if (destroyedItem == null) {
                                                PunishmentManager.handleIllegalPlayerAction(
                                                   player,
                                                   "Unable to delete item on enchant failure from " + player + ", possible cheater !",
                                                   GeneralConfig.DEFAULT_PUNISH
                                                );
                                                player.setActiveEnchantItemId(-1);
                                                player.sendPacket(new EnchantResult(2, 0, 0));
                                                if (GeneralConfig.LOG_ITEM_ENCHANTS) {
                                                   StringBuilder sb = new StringBuilder();
                                                   if (item.isEnchanted()) {
                                                      if (support == null) {
                                                         LOGGER_ENCHANT.info(
                                                            sb.append("Unable to destroy, Character:")
                                                               .append(player.getName())
                                                               .append(" [")
                                                               .append(player.getObjectId())
                                                               .append("] Account:")
                                                               .append(player.getAccountName())
                                                               .append(" IP:")
                                                               .append(player.getIPAddress())
                                                               .append(", +")
                                                               .append(item.getEnchantLevel())
                                                               .append(' ')
                                                               .append(item.getName())
                                                               .append('(')
                                                               .append(item.getCount())
                                                               .append(") [")
                                                               .append(item.getObjectId())
                                                               .append("], ")
                                                               .append(scroll.getName())
                                                               .append('(')
                                                               .append(scroll.getCount())
                                                               .append(") [")
                                                               .append(scroll.getObjectId())
                                                               .append(']')
                                                               .toString()
                                                         );
                                                      } else {
                                                         LOGGER_ENCHANT.info(
                                                            sb.append("Unable to destroy, Character:")
                                                               .append(player.getName())
                                                               .append(" [")
                                                               .append(player.getObjectId())
                                                               .append("] Account:")
                                                               .append(player.getAccountName())
                                                               .append(" IP:")
                                                               .append(player.getIPAddress())
                                                               .append(", +")
                                                               .append(item.getEnchantLevel())
                                                               .append(' ')
                                                               .append(item.getName())
                                                               .append('(')
                                                               .append(item.getCount())
                                                               .append(") [")
                                                               .append(item.getObjectId())
                                                               .append("], ")
                                                               .append(scroll.getName())
                                                               .append('(')
                                                               .append(scroll.getCount())
                                                               .append(") [")
                                                               .append(scroll.getObjectId())
                                                               .append("], ")
                                                               .append(support.getName())
                                                               .append('(')
                                                               .append(support.getCount())
                                                               .append(") [")
                                                               .append(support.getObjectId())
                                                               .append(']')
                                                               .toString()
                                                         );
                                                      }
                                                   } else if (support == null) {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Unable to destroy, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", ")
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   } else {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Unable to destroy, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", ")
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append("], ")
                                                            .append(support.getName())
                                                            .append('(')
                                                            .append(support.getCount())
                                                            .append(") [")
                                                            .append(support.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   }
                                                }

                                                return;
                                             }

                                             iu.addRemovedItem(destroyedItem);
                                             int crystalId = item.getTemplate().getCrystalItemId();
                                             if (crystalId != 0 && item.getTemplate().isCrystallizable()) {
                                                int count = item.getCrystalCount() - (item.getTemplate().getCrystalCount() + 1) / 2;
                                                count = count < 1 ? 1 : count;
                                                Item crystals = player.getInventory().addItem(ItemProcessType.COMPENSATE, crystalId, count, player, item);
                                                SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                                                sm.addItemName(crystalId);
                                                sm.addLong(count);
                                                player.sendPacket(sm);
                                                if (crystals.getLastChange() == 2) {
                                                   iu.addModifiedItem(crystals);
                                                } else {
                                                   iu.addNewItem(crystals);
                                                }

                                                player.sendPacket(new EnchantResult(1, crystalId, count));
                                             } else {
                                                player.sendPacket(new EnchantResult(4, 0, 0));
                                             }

                                             if (GeneralConfig.LOG_ITEM_ENCHANTS) {
                                                StringBuilder sb = new StringBuilder();
                                                if (item.isEnchanted()) {
                                                   if (support == null) {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Fail, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", +")
                                                            .append(item.getEnchantLevel())
                                                            .append(' ')
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   } else {
                                                      LOGGER_ENCHANT.info(
                                                         sb.append("Fail, Character:")
                                                            .append(player.getName())
                                                            .append(" [")
                                                            .append(player.getObjectId())
                                                            .append("] Account:")
                                                            .append(player.getAccountName())
                                                            .append(" IP:")
                                                            .append(player.getIPAddress())
                                                            .append(", +")
                                                            .append(item.getEnchantLevel())
                                                            .append(' ')
                                                            .append(item.getName())
                                                            .append('(')
                                                            .append(item.getCount())
                                                            .append(") [")
                                                            .append(item.getObjectId())
                                                            .append("], ")
                                                            .append(scroll.getName())
                                                            .append('(')
                                                            .append(scroll.getCount())
                                                            .append(") [")
                                                            .append(scroll.getObjectId())
                                                            .append("], ")
                                                            .append(support.getName())
                                                            .append('(')
                                                            .append(support.getCount())
                                                            .append(") [")
                                                            .append(support.getObjectId())
                                                            .append(']')
                                                            .toString()
                                                      );
                                                   }
                                                } else if (support == null) {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", ")
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                } else {
                                                   LOGGER_ENCHANT.info(
                                                      sb.append("Fail, Character:")
                                                         .append(player.getName())
                                                         .append(" [")
                                                         .append(player.getObjectId())
                                                         .append("] Account:")
                                                         .append(player.getAccountName())
                                                         .append(" IP:")
                                                         .append(player.getIPAddress())
                                                         .append(", ")
                                                         .append(item.getName())
                                                         .append('(')
                                                         .append(item.getCount())
                                                         .append(") [")
                                                         .append(item.getObjectId())
                                                         .append("], ")
                                                         .append(scroll.getName())
                                                         .append('(')
                                                         .append(scroll.getCount())
                                                         .append(") [")
                                                         .append(scroll.getObjectId())
                                                         .append("], ")
                                                         .append(support.getName())
                                                         .append('(')
                                                         .append(support.getCount())
                                                         .append(") [")
                                                         .append(support.getObjectId())
                                                         .append(']')
                                                         .toString()
                                                   );
                                                }
                                             }
                                          }
                                       }
                                 }

                                 player.sendInventoryUpdate(iu);
                                 player.broadcastUserInfo();
                                 player.setActiveEnchantItemId(-1);
                              } else {
                                 player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
                                 player.setActiveEnchantItemId(-1);
                                 player.sendPacket(new EnchantResult(2, 0, 0));
                              }
                           }
                        }
                     } else {
                        PunishmentManager.handleIllegalPlayerAction(player, player + " use autoenchant program ", GeneralConfig.DEFAULT_PUNISH);
                        player.setActiveEnchantItemId(-1);
                        player.sendPacket(new EnchantResult(2, 0, 0));
                     }
                  } else {
                     player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
                     player.setActiveEnchantItemId(-1);
                     player.sendPacket(new EnchantResult(2, 0, 0));
                  }
               }
            } else {
               player.setActiveEnchantItemId(-1);
               player.sendPacket(SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
               player.sendPacket(new EnchantResult(2, 0, 0));
            }
         } else {
            player.sendPacket(SystemMessageId.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
            player.setActiveEnchantItemId(-1);
         }
      }
   }
}
