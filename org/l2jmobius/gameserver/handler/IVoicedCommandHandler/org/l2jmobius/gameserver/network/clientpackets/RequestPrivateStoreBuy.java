package org.l2jmobius.gameserver.network.clientpackets;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.holders.RequestTrade;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestPrivateStoreBuy extends ClientPacket {
   private static final int BATCH_LENGTH = 20;
   private int _storePlayerId;
   private Set<RequestTrade> _items = null;

   @Override
   protected void readImpl() {
      this._storePlayerId = this.readInt();
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 20 == this.remaining()) {
         this._items = new HashSet<>();

         for (int i = 0; i < count; i++) {
            int objectId = this.readInt();
            long cnt = this.readLong();
            long price = this.readLong();
            if (objectId < 1 || cnt < 1L || price < 0L) {
               this._items = null;
               return;
            }

            this._items.add(new RequestTrade(objectId, cnt, price));
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
            player.sendMessage("You are buying items too fast.");
         } else {
            WorldObject object = World.getPlayer(this._storePlayerId);
            if (object != null) {
               if (!player.isCursedWeaponEquipped()) {
                  Player storePlayer = object.asPlayer();
                  if (player.isInsideRadius3D(storePlayer, 250)) {
                     if (player.getInstanceId() == storePlayer.getInstanceId() || player.getInstanceId() == -1) {
                        if (storePlayer.getPrivateStoreType() == PrivateStoreType.SELL || storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL) {
                           TradeList storeList = storePlayer.getSellList();
                           if (storeList != null) {
                              if (!player.getAccessLevel().allowTransaction()) {
                                 player.sendMessage("Transactions are disabled for your Access Level.");
                                 player.sendPacket(ActionFailed.STATIC_PACKET);
                              } else if (storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL && storeList.getItemCount() > this._items.size()) {
                                 String msgErr = "[RequestPrivateStoreBuy] "
                                    + player
                                    + " tried to buy less items than sold by package-sell, ban this player for bot usage!";
                                 PunishmentManager.handleIllegalPlayerAction(player, msgErr, GeneralConfig.DEFAULT_PUNISH);
                              } else {
                                 int result = storeList.privateStoreBuy(player, this._items);
                                 if (result > 0) {
                                    player.sendPacket(ActionFailed.STATIC_PACKET);
                                    if (result > 1) {
                                       PacketLogger.warning(
                                          "PrivateStore buy has failed due to invalid list or request. Player: "
                                             + player.getName()
                                             + ", Private store of: "
                                             + storePlayer.getName()
                                       );
                                    }
                                 } else {
                                    if (OfflineTradeConfig.OFFLINE_TRADE_ENABLE
                                       && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME
                                       && (storePlayer.getClient() == null || storePlayer.getClient().isDetached())) {
                                       OfflineTraderTable.getInstance().onTransaction(storePlayer, storeList.getItemCount() == 0, false);
                                    }

                                    if (storeList.getItemCount() == 0) {
                                       storePlayer.setPrivateStoreType(PrivateStoreType.NONE);
                                       storePlayer.broadcastUserInfo();
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
}
