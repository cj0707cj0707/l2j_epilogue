package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherAdd;
import org.l2jmobius.gameserver.network.serverpackets.TradeOwnAdd;
import org.l2jmobius.gameserver.network.serverpackets.TradeUpdate;

public class AddTradeItem extends ClientPacket {
   private int _tradeId;
   private int _objectId;
   private long _count;

   @Override
   protected void readImpl() {
      this._tradeId = this.readInt();
      this._objectId = this.readInt();
      this._count = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._count >= 1L) {
            TradeList trade = player.getActiveTradeList();
            if (trade == null) {
               PacketLogger.warning("Character: " + player.getName() + " requested item:" + this._objectId + " add without active tradelist:" + this._tradeId);
            } else {
               Player partner = trade.getPartner();
               if (partner != null && World.getPlayer(partner.getObjectId()) != null && partner.getActiveTradeList() != null) {
                  if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed()) {
                     player.sendPacket(SystemMessageId.YOU_MAY_NO_LONGER_ADJUST_ITEMS_IN_THE_TRADE_BECAUSE_THE_TRADE_HAS_BEEN_CONFIRMED);
                  } else if (!player.getAccessLevel().allowTransaction()) {
                     player.sendMessage("Transactions are disabled for your Access Level.");
                     player.cancelActiveTrade();
                  } else if (!player.validateItemManipulation(this._objectId, ItemProcessType.TRANSFER)) {
                     player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
                  } else {
                     TradeItem item = trade.addItem(this._objectId, this._count);
                     if (item != null) {
                        player.sendPacket(new TradeOwnAdd(item));
                        player.sendPacket(new TradeUpdate(trade, player));
                        trade.getPartner().sendPacket(new TradeOtherAdd(item));
                     }
                  }
               } else {
                  if (partner != null) {
                     PacketLogger.warning("Character:" + player.getName() + " requested invalid trade object: " + this._objectId);
                  }

                  player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
                  player.cancelActiveTrade();
               }
            }
         }
      }
   }
}
