package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationCancelResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

public class RequestRefineCancel extends ClientPacket {
   private int _targetItemObjId;

   @Override
   protected void readImpl() {
      this._targetItemObjId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item targetItem = player.getInventory().getItemByObjectId(this._targetItemObjId);
         if (targetItem == null) {
            player.sendPacket(new ExVariationCancelResult(0));
         } else if (targetItem.getOwnerId() != player.getObjectId()) {
            PunishmentManager.handleIllegalPlayerAction(
               player,
               "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tryied to augment item that doesn't own.",
               GeneralConfig.DEFAULT_PUNISH
            );
         } else if (!targetItem.isAugmented()) {
            player.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
            player.sendPacket(new ExVariationCancelResult(0));
         } else {
            int price = 0;
            switch (targetItem.getTemplate().getCrystalType()) {
               case C:
                  if (targetItem.getCrystalCount() < 1720) {
                     price = 95000;
                  } else if (targetItem.getCrystalCount() < 2452) {
                     price = 150000;
                  } else {
                     price = 210000;
                  }
                  break;
               case B:
                  if (targetItem.getCrystalCount() < 1746) {
                     price = 240000;
                  } else {
                     price = 270000;
                  }
                  break;
               case A:
                  if (targetItem.getCrystalCount() < 2160) {
                     price = 330000;
                  } else if (targetItem.getCrystalCount() < 2824) {
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
                  player.sendPacket(new ExVariationCancelResult(0));
                  return;
            }

            if (!player.reduceAdena(ItemProcessType.FEE, price, null, true)) {
               player.sendPacket(new ExVariationCancelResult(0));
               player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            } else {
               InventoryUpdate iu = new InventoryUpdate();
               if (targetItem.isEquipped()) {
                  for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot())) {
                     iu.addModifiedItem(itm);
                  }
               }

               targetItem.removeAugmentation();
               player.sendPacket(new ExVariationCancelResult(1));
               iu.addModifiedItem(targetItem);
               player.sendInventoryUpdate(iu);
            }
         }
      }
   }
}
