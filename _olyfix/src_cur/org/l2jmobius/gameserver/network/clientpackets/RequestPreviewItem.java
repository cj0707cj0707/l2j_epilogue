package org.l2jmobius.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ShopPreviewInfo;

public class RequestPreviewItem extends ClientPacket {
   private int _unk;
   private int _listId;
   private int _count;
   private int[] _items;

   @Override
   protected void readImpl() {
      this._unk = this.readInt();
      this._listId = this.readInt();
      this._count = this.readInt();
      if (this._count < 0) {
         this._count = 0;
      }

      if (this._count <= 100) {
         this._items = new int[this._count];

         for (int i = 0; i < this._count; i++) {
            this._items[i] = this.readInt();
         }
      }
   }

   @Override
   protected void runImpl() {
      if (this._items != null) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
               player.sendMessage("You are previewing items too fast.");
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!player.getAccessLevel().allowTransaction()) {
               player.sendMessage("Transactions are disabled for your access level.");
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP || player.getKarma() <= 0) {
               WorldObject target = player.getTarget();
               if (player.isGM() || target != null && target instanceof Merchant && player.isInsideRadius2D(target, 250)) {
                  if (this._count >= 1 && this._listId < 4000000) {
                     Merchant merchant = target instanceof Merchant ? (Merchant)target : null;
                     if (merchant == null) {
                        PacketLogger.warning(this.getClass().getName() + ": Null merchant!");
                     } else {
                        BuyListHolder buyList = BuyListData.getInstance().getBuyList(this._listId);
                        if (buyList == null) {
                           PunishmentManager.handleIllegalPlayerAction(
                              player,
                              "Warning!! Character "
                                 + player.getName()
                                 + " of account "
                                 + player.getAccountName()
                                 + " sent a false BuyList list_id "
                                 + this._listId,
                              GeneralConfig.DEFAULT_PUNISH
                           );
                        } else {
                           long totalPrice = 0L;
                           Map<Integer, Integer> itemList = new HashMap<>();

                           for (int i = 0; i < this._count; i++) {
                              int itemId = this._items[i];
                              Product product = buyList.getProductByItemId(itemId);
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
                                       + itemId,
                                    GeneralConfig.DEFAULT_PUNISH
                                 );
                                 return;
                              }

                              ItemTemplate template = product.getItem();
                              if (template != null) {
                                 int slot = Inventory.getPaperdollIndex(template.getBodyPart());
                                 if (slot >= 0
                                    && (
                                       template instanceof Weapon
                                          ? player.getRace().ordinal() != 5
                                             || template.getItemType() != WeaponType.NONE
                                                && template.getItemType() != WeaponType.RAPIER
                                                && template.getItemType() != WeaponType.CROSSBOW
                                                && template.getItemType() != WeaponType.ANCIENTSWORD
                                          : !(template instanceof Armor)
                                             || player.getRace().ordinal() != 5
                                             || template.getItemType() != ArmorType.HEAVY && template.getItemType() != ArmorType.MAGIC
                                    )) {
                                    if (itemList.containsKey(slot)) {
                                       player.sendPacket(SystemMessageId.YOU_CAN_NOT_TRY_THOSE_ITEMS_ON_AT_THE_SAME_TIME);
                                       return;
                                    }

                                    itemList.put(slot, itemId);
                                    totalPrice += GeneralConfig.WEAR_PRICE;
                                    if (totalPrice > Inventory.MAX_ADENA) {
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
                                 }
                              }
                           }

                           if (totalPrice >= 0L && player.reduceAdena(ItemProcessType.FEE, totalPrice, player.getLastFolkNPC(), true)) {
                              if (!itemList.isEmpty()) {
                                 player.sendPacket(new ShopPreviewInfo(itemList));
                                 ThreadPool.schedule(new RequestPreviewItem.RemoveWearItemsTask(player), GeneralConfig.WEAR_DELAY * 1000);
                              }
                           } else {
                              player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                           }
                        }
                     }
                  } else {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }

   private class RemoveWearItemsTask implements Runnable {
      private final Player _player;

      protected RemoveWearItemsTask(Player player) {
         Objects.requireNonNull(RequestPreviewItem.this);
         super();
         this._player = player;
      }

      @Override
      public void run() {
         try {
            this._player.sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_TRYING_ON_EQUIPMENT_2);
            this._player.updateUserInfo();
         } catch (Exception var2) {
            PacketLogger.warning(this.getClass().getSimpleName() + ": " + var2.getMessage());
         }
      }
   }
}
