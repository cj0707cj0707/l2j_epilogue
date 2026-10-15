package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.PremiumItem;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExGetPremiumItemList;

public class RequestWithDrawPremiumItem extends ClientPacket {
   private int _itemNum;
   private int _charId;
   private long _itemCount;

   @Override
   protected void readImpl() {
      this._itemNum = this.readInt();
      this._charId = this.readInt();
      this._itemCount = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._itemCount >= 1L) {
            if (player.getObjectId() != this._charId) {
               PunishmentManager.handleIllegalPlayerAction(
                  player, "[RequestWithDrawPremiumItem] Incorrect owner, Player: " + player.getName(), GeneralConfig.DEFAULT_PUNISH
               );
            } else if (player.getPremiumItemList().isEmpty()) {
               PunishmentManager.handleIllegalPlayerAction(
                  player, "[RequestWithDrawPremiumItem] Player: " + player.getName() + " try to get item with empty list!", GeneralConfig.DEFAULT_PUNISH
               );
            } else if (player.getWeightPenalty() >= 3 || !player.isInventoryUnder90(false)) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_THE_VITAMIN_ITEM_BECAUSE_YOU_HAVE_EXCEED_YOUR_INVENTORY_WEIGHT_QUANTITY_LIMIT);
            } else if (player.isProcessingTransaction()) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_A_VITAMIN_ITEM_DURING_AN_EXCHANGE);
            } else {
               PremiumItem item = player.getPremiumItemList().get(this._itemNum);
               if (item != null) {
                  if (item.getCount() >= this._itemCount) {
                     long itemsLeft = item.getCount() - this._itemCount;
                     player.addItem(ItemProcessType.TRANSFER, item.getItemId(), this._itemCount, player.getTarget(), true);
                     if (itemsLeft > 0L) {
                        item.updateCount(itemsLeft);
                        player.updatePremiumItem(this._itemNum, itemsLeft);
                     } else {
                        player.getPremiumItemList().remove(this._itemNum);
                        player.deletePremiumItem(this._itemNum);
                     }

                     if (player.getPremiumItemList().isEmpty()) {
                        player.sendPacket(SystemMessageId.THERE_ARE_NO_MORE_VITAMIN_ITEMS_TO_BE_FOUND);
                     } else {
                        player.sendPacket(new ExGetPremiumItemList(player));
                     }
                  }
               }
            }
         }
      }
   }
}
