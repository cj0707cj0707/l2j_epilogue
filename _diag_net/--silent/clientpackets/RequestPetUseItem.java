package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPetUseItem extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && player.hasPet()) {
         if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canUseItem()) {
            Pet pet = player.getSummon().asPet();
            Item item = pet.getInventory().getItemByObjectId(this._objectId);
            if (item != null) {
               if (!item.getTemplate().isForNpc()) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
               } else if (!player.isAlikeDead() && !pet.isDead()) {
                  int reuseDelay = item.getReuseDelay();
                  if (reuseDelay > 0) {
                     long reuse = pet.getItemRemainingReuseTime(item.getObjectId());
                     if (reuse > 0L) {
                        return;
                     }
                  }

                  if (item.isEquipped() || item.getTemplate().checkCondition(pet, pet, true)) {
                     this.useItem(pet, item, player);
                  }
               } else {
                  SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
                  sm.addItemName(item);
                  player.sendPacket(sm);
               }
            }
         }
      }
   }

   private void useItem(Pet pet, Item item, Player player) {
      if (item.isEquipable()) {
         if (!item.getTemplate().isConditionAttached()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
            return;
         }

         if (item.isEquipped()) {
            pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_TOOK_OFF_S1);
            sm.addItemName(item);
            player.sendPacket(sm);
         } else {
            pet.getInventory().equipItem(item);
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_PUT_ON_S1);
            sm.addItemName(item);
            player.sendPacket(sm);
         }

         player.sendPacket(new PetItemList(pet.getInventory().getItems()));
         pet.updateAndBroadcastStatus(1);
      } else {
         IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
         if (handler != null) {
            if (handler.onItemUse(pet, item, false)) {
               int reuseDelay = item.getReuseDelay();
               if (reuseDelay > 0) {
                  player.addTimeStampItem(item, reuseDelay);
               }

               pet.updateAndBroadcastStatus(1);
            }
         } else {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
            org.l2jmobius.gameserver.network.PacketLogger.warning("No item handler registered for itemId: " + item.getId());
         }
      }
   }
}
