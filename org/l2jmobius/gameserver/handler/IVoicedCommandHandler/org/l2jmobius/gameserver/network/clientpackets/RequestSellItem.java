package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.UniqueItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestSellItem extends ClientPacket {
   private static final int BATCH_LENGTH = 16;
   private static final int CUSTOM_CB_SELL_LIST = 423;
   private int _listId;
   private List<UniqueItemHolder> _items = null;

   @Override
   protected void readImpl() {
      this._listId = this.readInt();
      int size = this.readInt();
      if (size > 0 && size <= PlayerConfig.MAX_ITEM_IN_PACKET && size * 16 == this.remaining()) {
         this._items = new ArrayList<>(size);

         for (int i = 0; i < size; i++) {
            int objectId = this.readInt();
            int itemId = this.readInt();
            long count = this.readLong();
            if (objectId < 1 || itemId < 1 || count < 1L) {
               this._items = null;
               return;
            }

            this._items.add(new UniqueItemHolder(itemId, objectId, count));
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are selling too fast.");
         } else if (this._items == null) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            WorldObject target = player.getTarget();
            Creature merchant = null;
            if (!player.isGM() && this._listId != 423) {
               if (target == null || !player.isInsideRadius3D(target, 250) || player.getInstanceId() != target.getInstanceId()) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               if (!(target instanceof Merchant)) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               merchant = target.asCreature();
            }

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
               } else if (merchant != null && !buyList.isNpcAllowed(merchant.getId())) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  long totalPrice = 0L;

                  for (UniqueItemHolder i : this._items) {
                     Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
                     if (item != null && item.isSellable()) {
                        long price = item.getReferencePrice() / 2;
                        totalPrice += price * i.getCount();
                        if (Inventory.MAX_ADENA / i.getCount() < price || totalPrice > Inventory.MAX_ADENA) {
                           player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
                           return;
                        }

                        if (GeneralConfig.ALLOW_REFUND) {
                           player.getInventory().transferItem(ItemProcessType.TRANSFER, i.getObjectId(), i.getCount(), player.getRefund(), player, merchant);
                        } else {
                           player.getInventory().destroyItem(ItemProcessType.SELL, i.getObjectId(), i.getCount(), player, merchant);
                        }
                     }
                  }

                  if (!MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE) {
                     player.addAdena(ItemProcessType.SELL, totalPrice, merchant, false);
                  }

                  StatusUpdate su = new StatusUpdate(player);
                  su.addAttribute(14, player.getCurrentLoad());
                  player.sendPacket(su);
                  player.sendPacket(new ExBuySellList(player, buyList, true));
                  player.sendPacket(SystemMessageId.THE_TRANSACTION_IS_COMPLETE);
               }
            }
         }
      }
   }
}
