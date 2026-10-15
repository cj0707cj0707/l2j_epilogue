package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class SetPrivateStoreListSell extends ClientPacket {
   private static final int BATCH_LENGTH = 20;
   private boolean _packageSale;
   private SetPrivateStoreListSell.Item[] _items = null;

   @Override
   protected void readImpl() {
      this._packageSale = this.readInt() == 1;
      int count = this.readInt();
      if (count >= 1 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 20 == this.remaining()) {
         this._items = new SetPrivateStoreListSell.Item[count];

         for (int i = 0; i < count; i++) {
            int itemId = this.readInt();
            long cnt = this.readLong();
            long price = this.readLong();
            if (itemId < 1 || cnt < 1L || price < 0L) {
               this._items = null;
               return;
            }

            this._items[i] = new SetPrivateStoreListSell.Item(itemId, cnt, price);
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._items == null) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.INCORRECT_ITEM_COUNT);
            player.setPrivateStoreType(PrivateStoreType.NONE);
            player.broadcastUserInfo();
         } else if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         } else if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !player.isInDuel()) {
            if (player.isInsideZone(ZoneId.NO_STORE)) {
               player.sendPacket(new PrivateStoreManageListSell(player, this._packageSale));
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!player.canOpenPrivateStore()) {
               player.sendPacket(new PrivateStoreManageListSell(player, this._packageSale));
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (this._items.length > player.getPrivateSellStoreLimit()) {
               player.sendPacket(new PrivateStoreManageListSell(player, this._packageSale));
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
            } else {
               TradeList tradeList = player.getSellList();
               tradeList.clear();
               tradeList.setPackaged(this._packageSale);
               long totalCost = player.getAdena();

               for (SetPrivateStoreListSell.Item i : this._items) {
                  if (!i.addToTradeList(tradeList)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
                     return;
                  }

                  totalCost += i.getPrice();
                  if (totalCost > Inventory.MAX_ADENA) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
                     return;
                  }
               }

               player.sitDown();
               if (this._packageSale) {
                  player.setPrivateStoreType(PrivateStoreType.PACKAGE_SELL);
               } else {
                  player.setPrivateStoreType(PrivateStoreType.SELL);
               }

               player.broadcastUserInfo();
               if (this._packageSale) {
                  player.broadcastPacket(new ExPrivateStoreSetWholeMsg(player));
               } else {
                  player.broadcastPacket(new PrivateStoreMsgSell(player));
               }
            }
         } else {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP
            );
            player.sendPacket(new PrivateStoreManageListSell(player, this._packageSale));
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
            list.addItem(this._itemId, this._count, this._price);
            return true;
         }
      }

      public long getPrice() {
         return this._count * this._price;
      }
   }
}
