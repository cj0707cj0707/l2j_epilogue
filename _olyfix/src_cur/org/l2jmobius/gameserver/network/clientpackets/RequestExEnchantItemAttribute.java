package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.Elementals;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestExEnchantItemAttribute extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._objectId == -1) {
            player.setActiveEnchantAttrItemId(-1);
            player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
         } else if (!player.isOnline()) {
            player.setActiveEnchantAttrItemId(-1);
         } else if (player.isInStoreMode()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
            player.setActiveEnchantAttrItemId(-1);
         } else if (player.getActiveRequester() != null) {
            player.cancelActiveTrade();
            player.setActiveEnchantAttrItemId(-1);
            player.sendMessage("You cannot add elemental power while trading.");
         } else {
            Item item = player.getInventory().getItemByObjectId(this._objectId);
            Item stone = player.getInventory().getItemByObjectId(player.getActiveEnchantAttrItemId());
            if (item == null || stone == null) {
               player.setActiveEnchantAttrItemId(-1);
               player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
            } else if (!item.isElementable()) {
               player.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
               player.setActiveEnchantAttrItemId(-1);
            } else {
               switch (item.getItemLocation()) {
                  case INVENTORY:
                  case PAPERDOLL:
                     if (item.getOwnerId() != player.getObjectId()) {
                        player.setActiveEnchantAttrItemId(-1);
                        return;
                     } else {
                        int stoneId = stone.getId();
                        byte elementToAdd = Elementals.getItemElement(stoneId);
                        if (item.isArmor()) {
                           elementToAdd = Elementals.getOppositeElement(elementToAdd);
                        }

                        Elementals oldElement = item.getElemental(elementToAdd);
                        if ((!item.isWeapon() || oldElement == null || oldElement.getElement() == elementToAdd || oldElement.getElement() == -2)
                           && (item.getElemental(elementToAdd) != null || item.getElementals() == null || item.getElementals().length < 1)) {
                           int elementValue = oldElement == null ? 0 : oldElement.getValue();
                           byte opositeElement = Elementals.getOppositeElement(elementToAdd);
                           int limit = this.getLimit(item, stoneId);
                           int powerToAdd = this.getPowerToAdd(stoneId, elementValue, item);
                           if (item.isArmor() && item.getElementals() != null) {
                              for (Elementals elm : item.getElementals()) {
                                 if (elm.getElement() == opositeElement) {
                                    player.setActiveEnchantAttrItemId(-1);
                                    PunishmentManager.handleIllegalPlayerAction(
                                       player, player + " tried to add oposite attribute to item!", GeneralConfig.DEFAULT_PUNISH
                                    );
                                    return;
                                 }
                              }
                           }

                           int newPower = elementValue + powerToAdd;
                           if (newPower > limit) {
                              newPower = limit;
                              powerToAdd = limit - elementValue;
                           }

                           if (powerToAdd <= 0) {
                              player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
                              player.setActiveEnchantAttrItemId(-1);
                              return;
                           } else if (!player.destroyItem(ItemProcessType.FEE, stone, 1L, player, true)) {
                              player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
                              PunishmentManager.handleIllegalPlayerAction(
                                 player, player + " tried to attribute enchant with a stone he doesn't have", GeneralConfig.DEFAULT_PUNISH
                              );
                              player.setActiveEnchantAttrItemId(-1);
                              return;
                           } else {
                              boolean success = false;
                              switch (Elementals.getItemElemental(stoneId).getType()) {
                                 case STONE:
                                 case ROUGH_ORE:
                                    success = Rnd.get(100) < PlayerConfig.ENCHANT_CHANCE_ELEMENT_STONE;
                                    break;
                                 case CRYSTAL:
                                    success = Rnd.get(100) < PlayerConfig.ENCHANT_CHANCE_ELEMENT_CRYSTAL;
                                    break;
                                 case JEWEL:
                                    success = Rnd.get(100) < PlayerConfig.ENCHANT_CHANCE_ELEMENT_JEWEL;
                                    break;
                                 case ENERGY:
                                    success = Rnd.get(100) < PlayerConfig.ENCHANT_CHANCE_ELEMENT_ENERGY;
                              }

                              if (success) {
                                 byte realElement = item.isArmor() ? opositeElement : elementToAdd;
                                 SystemMessage sm;
                                 if (item.getEnchantLevel() == 0) {
                                    sm = new SystemMessage(SystemMessageId.S2_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1);
                                    sm.addItemName(item);
                                    sm.addElemental(realElement);
                                 } else {
                                    sm = new SystemMessage(SystemMessageId.S3_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1_S2);
                                    sm.addInt(item.getEnchantLevel());
                                    sm.addItemName(item);
                                    sm.addElemental(realElement);
                                 }

                                 player.sendPacket(sm);
                                 item.setElementAttr(elementToAdd, newPower);
                                 if (item.isEquipped()) {
                                    item.updateElementAttrBonus(player);
                                 }

                                 InventoryUpdate iu = new InventoryUpdate();
                                 iu.addModifiedItem(item);
                                 player.sendInventoryUpdate(iu);
                              } else {
                                 player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
                              }

                              player.sendPacket(new ExAttributeEnchantResult(powerToAdd));
                              player.updateUserInfo();
                              player.setActiveEnchantAttrItemId(-1);
                              return;
                           }
                        } else {
                           player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED);
                           player.setActiveEnchantAttrItemId(-1);
                           return;
                        }
                     }
                  default:
                     player.setActiveEnchantAttrItemId(-1);
                     PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use enchant Exploit!", GeneralConfig.DEFAULT_PUNISH);
               }
            }
         }
      }
   }

   public int getLimit(Item item, int sotneId) {
      ElementalItemHolder elementItem = Elementals.getItemElemental(sotneId);
      if (elementItem == null) {
         return 0;
      } else {
         return item.isWeapon() ? Elementals.WEAPON_VALUES[elementItem.getType().getMaxLevel()] : Elementals.ARMOR_VALUES[elementItem.getType().getMaxLevel()];
      }
   }

   public int getPowerToAdd(int stoneId, int oldValue, Item item) {
      if (Elementals.getItemElement(stoneId) != -1) {
         if (item.isWeapon()) {
            if (oldValue == 0) {
               return 20;
            }

            return 5;
         }

         if (item.isArmor()) {
            return 6;
         }
      }

      return 0;
   }
}
