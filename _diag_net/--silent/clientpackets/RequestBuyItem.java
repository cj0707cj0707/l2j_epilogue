package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestBuyItem extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private static final int CUSTOM_CB_SELL_LIST = 423;
   private int _listId;
   private List<ItemHolder> _items = null;

   @Override
   protected void readImpl() {
      this._listId = this.readInt();
      int size = this.readInt();
      if (size > 0 && size <= PlayerConfig.MAX_ITEM_IN_PACKET && size * 12 == this.remaining()) {
         this._items = new ArrayList<>(size);

         for (int i = 0; i < size; i++) {
            int itemId = this.readInt();
            long count = this.readLong();
            if (itemId < 1 || count < 1L) {
               this._items = null;
               return;
            }

            this._items.add(new ItemHolder(itemId, count));
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are buying too fast.");
         } else if (this._items == null) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            WorldObject target = player.getTarget();
            Creature merchant = null;
            if (!player.isGM() && this._listId != 423) {
               if (!(target instanceof Merchant) || !player.isInsideRadius3D(target, 250) || player.getInstanceId() != target.getInstanceId()) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               merchant = target.asCreature();
            }

            double castleTaxRate = 0.0;
            double baseTaxRate = 0.0;
            if (merchant == null && !player.isGM() && this._listId != 423) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               BuyListHolder buyList = BuyListData.getInstance().getBuyList(this._listId);
               if (buyList == null) {
                  PunishmentManager.handleIllegalPlayerAction(
                     player,
                     "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + this._listId,
                     GeneralConfig.DEFAULT_PUNISH
                  );
               } else {
                  if (merchant != null) {
                     if (!buyList.isNpcAllowed(merchant.getId())) {
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }

                     if (merchant instanceof Merchant) {
                        castleTaxRate = ((Merchant)merchant).getMpc().getCastleTaxRate();
                        baseTaxRate = ((Merchant)merchant).getMpc().getBaseTaxRate();
                     } else {
                        baseTaxRate = 0.5;
                     }
                  }

                  long subTotal = 0L;
                  long slots = 0L;
                  long weight = 0L;

                  for (ItemHolder i : this._items) {
                     long price = -1L;
                     Product product = buyList.getProductByItemId(i.getId());
                     if (product == null) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " sent a false BuyList list_id "
                              + this._listId
                              + " and item_id "
                              + i.getId(),
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }

                     if (!product.getItem().isStackable() && i.getCount() > 1L) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to purchase invalid quantity of items at the same time.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
                        return;
                     }

                     price = product.getPrice();
                     if (product.getItemId() >= 3960 && product.getItemId() <= 4026) {
                        price = (long)((float)price * RatesConfig.RATE_SIEGE_GUARDS_PRICE);
                     }

                     if (price < 0L) {
                        org.l2jmobius.gameserver.network.PacketLogger.warning("ERROR, no price found .. wrong buylist ??");
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }

                     if (price == 0L && !player.isGM() && GeneralConfig.ONLY_GM_ITEMS_FREE) {
                        player.sendMessage("Ohh Cheat does not work? You have a problem now!");
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried buy item for 0 adena.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }

                     if (product.hasLimitedStock() && i.getCount() > product.getCount()) {
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }

                     if (Inventory.MAX_ADENA / i.getCount() < price) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to purchase over "
                              + Inventory.MAX_ADENA
                              + " adena worth of goods.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }

                     price = (long)(price * (1.0 + castleTaxRate + baseTaxRate));
                     subTotal += i.getCount() * price;
                     if (subTotal > Inventory.MAX_ADENA) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to purchase over "
                              + Inventory.MAX_ADENA
                              + " adena worth of goods.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }

                     weight += i.getCount() * product.getItem().getWeight();
                     if (player.getInventory().getItemByItemId(product.getItemId()) == null) {
                        slots++;
                     }
                  }

                  if (!player.isGM() && (weight > 2147483647L || weight < 0L || !player.getInventory().validateWeight((int)weight))) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  } else if (!player.isGM() && (slots > 2147483647L || slots < 0L || !player.getInventory().validateCapacity((int)slots))) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  } else if (subTotal >= 0L && player.reduceAdena(ItemProcessType.BUY, subTotal, player.getLastFolkNPC(), false)) {
                     for (ItemHolder i : this._items) {
                        Product productx = buyList.getProductByItemId(i.getId());
                        if (productx == null) {
                           PunishmentManager.handleIllegalPlayerAction(
                              player,
                              "Warning!! Character "
                                 + player.getName()
                                 + " of account "
                                 + player.getAccountName()
                                 + " sent a false BuyList list_id "
                                 + this._listId
                                 + " and item_id "
                                 + i.getId(),
                              GeneralConfig.DEFAULT_PUNISH
                           );
                        } else if (productx.hasLimitedStock()) {
                           if (productx.decreaseCount(i.getCount())) {
                              player.getInventory().addItem(ItemProcessType.BUY, i.getId(), i.getCount(), player, merchant);
                           }
                        } else {
                           player.getInventory().addItem(ItemProcessType.BUY, i.getId(), i.getCount(), player, merchant);
                        }
                     }

                     if (merchant instanceof Merchant) {
                        ((Merchant)merchant).getCastle().addToTreasury((long)(subTotal * castleTaxRate));
                     }

                     StatusUpdate su = new StatusUpdate(player);
                     su.addAttribute(14, player.getCurrentLoad());
                     player.sendPacket(su);
                     player.sendPacket(new ExBuySellList(player, buyList, castleTaxRate + baseTaxRate, true));
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_TRANSACTION_IS_COMPLETE);
                  } else {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }
}
