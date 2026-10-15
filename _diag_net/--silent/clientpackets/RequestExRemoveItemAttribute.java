package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.Elementals;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExShowBaseAttributeCancelWindow;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

public class RequestExRemoveItemAttribute extends ClientPacket {
   private int _objectId;
   private long _price;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item targetItem = player.getInventory().getItemByObjectId(this._objectId);
         if (targetItem != null) {
            if (targetItem.getElementals() != null) {
               if (player.reduceAdena(ItemProcessType.FEE, this.getPrice(targetItem), player, true)) {
                  if (targetItem.isEquipped()) {
                     targetItem.removeElementAttrBonus(player);
                  }

                  for (Elementals element : targetItem.getElementals()) {
                     targetItem.clearElementAttr(element.getElement());
                  }

                  player.updateUserInfo();
                  InventoryUpdate iu = new InventoryUpdate();
                  iu.addModifiedItem(targetItem);
                  player.sendInventoryUpdate(iu);
                  player.sendMessage(targetItem.getName() + "'s elemental power was removed.");
                  player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
               } else {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
               }
            }
         }
      }
   }

   private long getPrice(Item item) {
      switch (item.getTemplate().getCrystalType()) {
         case S:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 50000L;
            } else {
               this._price = 40000L;
            }
            break;
         case S80:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 100000L;
            } else {
               this._price = 80000L;
            }
            break;
         case S84:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 200000L;
            } else {
               this._price = 160000L;
            }
      }

      return this._price;
   }
}
