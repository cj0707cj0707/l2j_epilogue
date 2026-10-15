package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Collection;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestUnEquipItem extends ClientPacket {
   private BodyPart _slot;

   @Override
   protected void readImpl() {
      this._slot = BodyPart.fromPaperdollSlot(this.readInt());
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item item = player.getInventory().getPaperdollItemByBodyPart(this._slot);
         if (item != null) {
            if (player.isAttackingNow() || player.isCastingNow() || player.isCastingSimultaneouslyNow()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.DURING_AN_ATTACK_ONE_CANNOT_CHANGE_WEAPONS);
            } else if (this._slot != BodyPart.L_HAND || !(item.getTemplate() instanceof EtcItem)) {
               if (this._slot != BodyPart.LR_HAND || !player.isCursedWeaponEquipped() && !player.isCombatFlagEquipped()) {
                  if (!player.isStunned() && !player.isSleeping() && !player.isParalyzed() && !player.isAlikeDead()) {
                     if (!player.getInventory().canManipulateWithItemId(item.getId())) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_ITEM_CANNOT_BE_TAKEN_OFF);
                     } else if (item.isWeapon() && item.getWeaponItem().isForceEquip() && !player.isGM()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_ITEM_CANNOT_BE_TAKEN_OFF);
                     } else {
                        Collection<Item> unequipped = player.getInventory().unEquipItemInBodySlotAndRecord(this._slot);
                        player.broadcastUserInfo();
                        if (!unequipped.isEmpty()) {
                           SystemMessage sm = null;
                           Item unequippedItem = unequipped.iterator().next();
                           if (unequippedItem.isEnchanted()) {
                              sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                              sm.addInt(unequippedItem.getEnchantLevel());
                           } else {
                              sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_BEEN_DISARMED);
                           }

                           sm.addItemName(unequippedItem);
                           player.sendPacket(sm);
                           InventoryUpdate iu = new InventoryUpdate();
                           iu.addItems(unequipped);
                           player.sendInventoryUpdate(iu);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
