package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.holders.RequestTrade;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestPrivateStoreSell extends ClientPacket {
   private static final int BATCH_LENGTH = 28;
   private int _storePlayerId;
   private RequestTrade[] _items = null;

   @Override
   protected void readImpl() {
      this._storePlayerId = this.readInt();
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 28 == this.remaining()) {
         this._items = new RequestTrade[count];

         for (int i = 0; i < count; i++) {
            int objectId = this.readInt();
            int itemId = this.readInt();
            this.readShort();
            this.readShort();
            long cnt = this.readLong();
            long price = this.readLong();
            if (objectId < 1 || itemId < 1 || cnt < 1L || price < 0L) {
               this._items = null;
               return;
            }

            this._items[i] = new RequestTrade(objectId, itemId, cnt, price);
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._items == null) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (!this.getClient().getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are selling items too fast.");
         } else {
            Player object = World.getPlayer(this._storePlayerId);
            if (object != null) {
               if (player.isInsideRadius3D(object, 250)) {
                  if (player.getInstanceId() == object.getInstanceId() || player.getInstanceId() == -1) {
                     if (object.getPrivateStoreType() == PrivateStoreType.BUY) {
                        if (!player.isCursedWeaponEquipped()) {
                           TradeList storeList = object.getBuyList();
                           if (storeList != null) {
                              if (!player.getAccessLevel().allowTransaction()) {
                                 player.sendMessage("Transactions are disabled for your Access Level.");
                                 player.sendPacket(ActionFailed.STATIC_PACKET);
                              } else if (!storeList.privateStoreSell(player, this._items)) {
                                 player.sendPacket(ActionFailed.STATIC_PACKET);
                                 PacketLogger.warning(
                                    "PrivateStore sell has failed due to invalid list or request. Player: "
                                       + player.getName()
                                       + ", Private store of: "
                                       + object.getName()
                                 );
                              } else {
                                 if (OfflineTradeConfig.OFFLINE_TRADE_ENABLE
                                    && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME
                                    && (object.getClient() == null || object.getClient().isDetached())) {
                                    OfflineTraderTable.getInstance().onTransaction(object, storeList.getItemCount() == 0, false);
                                 }

                                 if (storeList.getItemCount() == 0) {
                                    object.setPrivateStoreType(PrivateStoreType.NONE);
                                    object.broadcastUserInfo();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
