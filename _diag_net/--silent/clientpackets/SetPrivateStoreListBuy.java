package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class SetPrivateStoreListBuy extends ClientPacket {
   private static final int BATCH_LENGTH = 40;
   private SetPrivateStoreListBuy.Item[] _items = null;

   @Override
   protected void readImpl() {
      int count = this.readInt();
      if (count >= 1 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 40 == this.remaining()) {
         this._items = new SetPrivateStoreListBuy.Item[count];

         for (int i = 0; i < count; i++) {
            int itemId = this.readInt();
            this.readInt();
            long cnt = this.readLong();
            long price = this.readLong();
            if (itemId < 1 || cnt < 1L || price < 0L) {
               this._items = null;
               return;
            }

            this.readInt();
            this.readInt();
            this.readInt();
            this.readInt();
            this._items[i] = new SetPrivateStoreListBuy.Item(itemId, cnt, price);
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._items == null) {
            player.setPrivateStoreType(PrivateStoreType.NONE);
            player.broadcastUserInfo();
         } else if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         } else if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !player.isInDuel()) {
            if (player.isInsideZone(ZoneId.NO_STORE)) {
               player.sendPacket(new PrivateStoreManageListBuy(player));
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!player.canOpenPrivateStore()) {
               player.sendPacket(new PrivateStoreManageListBuy(player));
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               TradeList tradeList = player.getBuyList();
               tradeList.clear();
               if (this._items.length > player.getPrivateBuyStoreLimit()) {
                  player.sendPacket(new PrivateStoreManageListBuy(player));
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
               } else {
                  long totalCost = 0L;

                  for (SetPrivateStoreListBuy.Item i : this._items) {
                     if (!i.addToTradeList(tradeList)) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to set price more than "
                              + Inventory.MAX_ADENA
                              + " adena in Private Store - Buy.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }

                     totalCost += i.getCost();
                     if (totalCost > Inventory.MAX_ADENA) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           "Warning!! Character "
                              + player.getName()
                              + " of account "
                              + player.getAccountName()
                              + " tried to set total price more than "
                              + Inventory.MAX_ADENA
                              + " adena in Private Store - Buy.",
                           GeneralConfig.DEFAULT_PUNISH
                        );
                        return;
                     }
                  }

                  if (totalCost > player.getAdena()) {
                     player.sendPacket(new PrivateStoreManageListBuy(player));
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_THE_AMOUNT_OF_MONEY_THAT_YOU_HAVE_AND_SO_YOU_CANNOT_OPEN_A_PERSONAL_STORE
                     );
                  } else {
                     player.sitDown();
                     player.setPrivateStoreType(PrivateStoreType.BUY);
                     player.broadcastUserInfo();
                     player.broadcastPacket(new PrivateStoreMsgBuy(player));
                  }
               }
            }
         } else {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP
            );
            player.sendPacket(new PrivateStoreManageListBuy(player));
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }

   private static class Item {
      private final int _itemId;
      private final long _count;
      private final long _price;

      public Item(int id, long num, long pri) {
         this._itemId = id;
         this._count = num;
         this._price = pri;
      }

      public boolean addToTradeList(TradeList list) {
         if (Inventory.MAX_ADENA / this._count < this._price) {
            return false;
         } else {
            list.addItemByItemId(this._itemId, this._count, this._price);
            return true;
         }
      }

      public long getCost() {
         return this._count * this._price;
      }
   }
}
