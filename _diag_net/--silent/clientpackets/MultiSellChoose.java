package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.Elementals;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.multisell.Entry;
import org.l2jmobius.gameserver.mechanics.multisell.Ingredient;
import org.l2jmobius.gameserver.mechanics.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class MultiSellChoose extends ClientPacket {
   private int _listId;
   private int _entryId;
   private long _amount;
   private int _unk1;
   private int _unk2;
   private int _unk3;
   private int _unk4;
   private int _unk5;
   private int _unk6;
   private int _unk7;
   private int _unk8;
   private int _unk9;
   private int _unk10;
   private int _unk11;

   @Override
   protected void readImpl() {
      this._listId = this.readInt();
      this._entryId = this.readInt();
      this._amount = this.readLong();
      this._unk1 = this.readShort();
      this._unk2 = this.readInt();
      this._unk3 = this.readInt();
      this._unk4 = this.readShort();
      this._unk5 = this.readShort();
      this._unk6 = this.readShort();
      this._unk7 = this.readShort();
      this._unk8 = this.readShort();
      this._unk9 = this.readShort();
      this._unk10 = this.readShort();
      this._unk11 = this.readShort();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canUseMultiSell()) {
            player.setMultiSell(null);
         } else if (this._amount >= 1L && this._amount <= GeneralConfig.MULTISELL_AMOUNT_LIMIT) {
            PreparedListContainer list = player.getMultiSell();
            if (list != null && list.getListId() == this._listId) {
               Npc npc = player.getLastFolkNPC();
               if (!list.isNpcAllowed(-1)
                  && (
                     npc == null
                        || !list.isNpcAllowed(npc.getId())
                        || !list.checkNpcObjectId(npc.getObjectId())
                        || player.getInstanceId() != npc.getInstanceId()
                        || !player.isInsideRadius3D(npc, 250)
                  )) {
                  if (!player.isGM()) {
                     player.setMultiSell(null);
                     return;
                  }

                  player.sendMessage("Multisell " + this._listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
               }

               for (Entry entry : list.getEntries()) {
                  if (entry.getEntryId() == this._entryId) {
                     if (!entry.isStackable() && this._amount > 1L) {
                        org.l2jmobius.gameserver.network.PacketLogger.warning(
                           "Character: "
                              + player.getName()
                              + " is trying to set amount > 1 on non-stackable multisell, id:"
                              + this._listId
                              + ":"
                              + this._entryId
                        );
                        player.setMultiSell(null);
                        return;
                     }

                     PlayerInventory inv = player.getInventory();
                     int slots = 0;
                     int weight = 0;

                     for (Ingredient e : entry.getProducts()) {
                        if (e.getItemId() >= 0) {
                           if (!e.isStackable()) {
                              slots = (int)(slots + e.getItemCount() * this._amount);
                           } else if (player.getInventory().getItemByItemId(e.getItemId()) == null) {
                              slots++;
                           }

                           weight = (int)(weight + e.getItemCount() * this._amount * e.getWeight());
                        }
                     }

                     if (!inv.validateWeight(weight)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                        return;
                     }

                     if (!inv.validateCapacity(slots)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
                        return;
                     }

                     List<Ingredient> ingredientsList = new ArrayList<>(entry.getIngredients().size());

                     for (Ingredient ex : entry.getIngredients()) {
                        boolean newIng = true;
                        int i = ingredientsList.size();

                        while (--i >= 0) {
                           Ingredient exx = ingredientsList.get(i);
                           if (exx.getItemId() == ex.getItemId() && exx.getEnchantLevel() == ex.getEnchantLevel()) {
                              if (exx.getItemCount() + ex.getItemCount() > 2147483647L) {
                                 player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
                                 return;
                              }

                              Ingredient ing = exx.getCopy();
                              ing.setItemCount(exx.getItemCount() + ex.getItemCount());
                              ingredientsList.set(i, ing);
                              newIng = false;
                              break;
                           }
                        }

                        if (newIng) {
                           ingredientsList.add(ex);
                        }
                     }

                     for (Ingredient ex : ingredientsList) {
                        if (ex.getItemCount() * this._amount > 2147483647L) {
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
                           return;
                        }

                        if (ex.getItemId() < 0) {
                           if (!MultisellData.hasSpecialIngredient(ex.getItemId(), ex.getItemCount() * this._amount, player)) {
                              return;
                           }
                        } else {
                           long required = !PlayerConfig.ALT_BLACKSMITH_USE_RECIPES && ex.getMaintainIngredient()
                              ? ex.getItemCount()
                              : ex.getItemCount() * this._amount;
                           if (inv.getInventoryItemCount(
                                 ex.getItemId(), !list.getMaintainEnchantment() && ex.getEnchantLevel() <= 0 ? -1 : ex.getEnchantLevel(), false
                              )
                              < required) {
                              SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S2_UNIT_S_OF_THE_ITEM_S1_IS_ARE_REQUIRED);
                              sm.addItemName(ex.getTemplate());
                              sm.addLong(required);
                              player.sendPacket(sm);
                              return;
                           }
                        }
                     }

                     List<Augmentation> augmentation = new ArrayList<>();
                     Elementals[] elemental = null;

                     for (Ingredient ex : entry.getIngredients()) {
                        if (ex.getItemId() < 0) {
                           if (!MultisellData.takeSpecialIngredient(ex.getItemId(), ex.getItemCount() * this._amount, player)) {
                              return;
                           }
                        } else {
                           Item itemToTake = inv.getItemByItemId(ex.getItemId());
                           if (itemToTake == null) {
                              org.l2jmobius.gameserver.network.PacketLogger.warning(
                                 "Character: " + player.getName() + " is trying to cheat in multisell, id:" + this._listId + ":" + this._entryId
                              );
                              player.setMultiSell(null);
                              return;
                           }

                           if (PlayerConfig.ALT_BLACKSMITH_USE_RECIPES || !ex.getMaintainIngredient()) {
                              if (itemToTake.isStackable()) {
                                 if (!player.destroyItem(
                                    ItemProcessType.FEE, itemToTake.getObjectId(), ex.getItemCount() * this._amount, player.getTarget(), true
                                 )) {
                                    player.setMultiSell(null);
                                    return;
                                 }
                              } else if (!list.getMaintainEnchantment() && ex.getEnchantLevel() <= 0) {
                                 for (int i = 1; i <= ex.getItemCount() * this._amount; i++) {
                                    Collection<Item> inventoryContents = inv.getAllItemsByItemId(ex.getItemId(), false);
                                    itemToTake = inventoryContents.iterator().next();
                                    if (itemToTake.isEnchanted()) {
                                       for (Item item : inventoryContents) {
                                          if (item.getEnchantLevel() < itemToTake.getEnchantLevel()) {
                                             itemToTake = item;
                                             if (item.getEnchantLevel() == 0) {
                                                break;
                                             }
                                          }
                                       }
                                    }

                                    if (!player.destroyItem(ItemProcessType.FEE, itemToTake.getObjectId(), 1L, player.getTarget(), true)) {
                                       player.setMultiSell(null);
                                       return;
                                    }
                                 }
                              } else {
                                 Collection<Item> inventoryContentsx = inv.getAllItemsByItemId(ex.getItemId(), ex.getEnchantLevel(), false);
                                 Iterator<Item> it = inventoryContentsx.iterator();

                                 for (int i = 0; i < ex.getItemCount() * this._amount && it.hasNext(); i++) {
                                    Item current = it.next();
                                    if (current.isAugmented()) {
                                       augmentation.add(current.getAugmentation());
                                    }

                                    if (current.getElementals() != null) {
                                       elemental = current.getElementals();
                                    }

                                    if (!player.destroyItem(ItemProcessType.FEE, current.getObjectId(), 1L, player.getTarget(), true)) {
                                       player.setMultiSell(null);
                                       return;
                                    }
                                 }
                              }
                           }
                        }
                     }

                     for (Ingredient exx : entry.getProducts()) {
                        if (exx.getItemId() < 0) {
                           MultisellData.giveSpecialProduct(exx.getItemId(), exx.getItemCount() * this._amount, player);
                        } else {
                           if (exx.isStackable()) {
                              inv.addItem(ItemProcessType.BUY, exx.getItemId(), exx.getItemCount() * this._amount, player, player.getTarget());
                           } else {
                              Item product = null;

                              for (int i = 0; i < exx.getItemCount() * this._amount; i++) {
                                 product = inv.addItem(ItemProcessType.BUY, exx.getItemId(), 1L, player, player.getTarget());
                                 if (product != null && (list.getMaintainEnchantment() || exx.getEnchantLevel() > 0)) {
                                    if (i < augmentation.size()) {
                                       product.setAugmentation(new Augmentation(augmentation.get(i).getAugmentationId()));
                                    }

                                    if (elemental != null) {
                                       for (Elementals elm : elemental) {
                                          product.setElementAttr(elm.getElement(), elm.getValue());
                                       }
                                    }

                                    product.setEnchantLevel(exx.getEnchantLevel());
                                    product.updateDatabase();
                                 }
                              }
                           }

                           if (exx.getItemCount() * this._amount > 1L) {
                              SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                              sm.addItemName(exx.getItemId());
                              sm.addLong(exx.getItemCount() * this._amount);
                              player.sendPacket(sm);
                           } else {
                              SystemMessage sm;
                              if (list.getMaintainEnchantment() && exx.getEnchantLevel() > 0) {
                                 sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.ACQUIRED_S1_S2);
                                 sm.addLong(exx.getEnchantLevel());
                                 sm.addItemName(exx.getItemId());
                              } else {
                                 sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EARNED_S1);
                                 sm.addItemName(exx.getItemId());
                              }

                              player.sendPacket(sm);
                           }
                        }
                     }

                     player.sendItemList(false);
                     StatusUpdate su = new StatusUpdate(player);
                     su.addAttribute(14, player.getCurrentLoad());
                     player.sendPacket(su);
                     if (npc != null && entry.getTaxAmount() > 0L) {
                        npc.getCastle().addToTreasury(entry.getTaxAmount() * this._amount);
                     }
                     break;
                  }
               }
            } else {
               player.setMultiSell(null);
            }
         } else {
            player.setMultiSell(null);
         }
      }
   }
}
