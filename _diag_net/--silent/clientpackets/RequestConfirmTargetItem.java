package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExPutItemResultForVariationMake;

public class RequestConfirmTargetItem extends AbstractRefinePacket {
   private int _itemObjId;

   @Override
   protected void readImpl() {
      this._itemObjId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item item = player.getInventory().getItemByObjectId(this._itemObjId);
         if (item != null) {
            if (!isValid(player, item)) {
               if (item.isAugmented()) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN);
               } else {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
               }
            } else {
               player.sendPacket(new ExPutItemResultForVariationMake(this._itemObjId, item.getId()));
            }
         }
      }
   }
}
