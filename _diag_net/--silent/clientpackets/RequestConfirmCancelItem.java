package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;

public class RequestConfirmCancelItem extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item item = player.getInventory().getItemByObjectId(this._objectId);
         if (item != null) {
            if (item.getOwnerId() != player.getObjectId()) {
               PunishmentManager.handleIllegalPlayerAction(
                  player,
                  "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tryied to destroy augment on item that doesn't own.",
                  GeneralConfig.DEFAULT_PUNISH
               );
            } else if (!item.isAugmented()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
            } else if (item.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
            } else {
               int price = 0;
               switch (item.getTemplate().getCrystalType()) {
                  case C:
                     if (item.getCrystalCount() < 1720) {
                        price = 95000;
                     } else if (item.getCrystalCount() < 2452) {
                        price = 150000;
                     } else {
                        price = 210000;
                     }
                     break;
                  case B:
                     if (item.getCrystalCount() < 1746) {
                        price = 240000;
                     } else {
                        price = 270000;
                     }
                     break;
                  case A:
                     if (item.getCrystalCount() < 2160) {
                        price = 330000;
                     } else if (item.getCrystalCount() < 2824) {
                        price = 390000;
                     } else {
                        price = 420000;
                     }
                     break;
                  case S:
                     price = 480000;
                     break;
                  case S80:
                  case S84:
                     price = 920000;
                     break;
                  default:
                     return;
               }

               player.sendPacket(new ExPutItemResultForVariationCancel(item, price));
            }
         }
      }
   }
}
