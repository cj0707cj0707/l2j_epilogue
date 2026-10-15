package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;

public class RequestGiveItemToPet extends ClientPacket {
   private int _objectId;
   private long _amount;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._amount = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (this._amount > 0L && player != null && player.hasPet()) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are giving items to pet too fast.");
         } else if (player.getActiveEnchantItemId() != -1) {
            player.setActiveEnchantItemId(-1);
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
            player.sendPacket(new EnchantResult(2, 0, 0));
         } else if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TRADE || player.getKarma() <= 0) {
            if (player.isInStoreMode()) {
               player.sendMessage("You cannot give items while trading.");
            } else {
               Item item = player.getInventory().getItemByObjectId(this._objectId);
               if (item != null) {
                  if (this._amount > item.getCount()) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        this.getClass().getSimpleName()
                           + ": Character "
                           + player.getName()
                           + " of account "
                           + player.getAccountName()
                           + " tried to give item with oid "
                           + this._objectId
                           + " to pet but has invalid count "
                           + this._amount
                           + " item count: "
                           + item.getCount(),
                        GeneralConfig.DEFAULT_PUNISH
                     );
                  } else if (!item.isAugmented()) {
                     if (!item.isHeroItem() && item.isDropable() && item.isDestroyable() && item.isTradeable()) {
                        Pet pet = player.getSummon().asPet();
                        if (pet.isDead()) {
                           player.sendPacket(
                              org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_IS_MOTIONLESS_AND_ANY_ATTEMPT_YOU_MAKE_TO_GIVE_IT_SOMETHING_GOES_UNRECOGNIZED
                           );
                        } else if (!pet.getInventory().validateCapacity(item)) {
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS_REMOVE_SOME_THEN_TRY_AGAIN);
                        } else if (!pet.getInventory().validateWeight(item, this._amount)) {
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
                        } else {
                           if (player.transferItem(ItemProcessType.TRANSFER, this._objectId, this._amount, pet.getInventory(), pet) == null) {
                              org.l2jmobius.gameserver.network.PacketLogger.warning(
                                 "Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName()
                              );
                           }
                        }
                     } else {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
                     }
                  }
               }
            }
         }
      }
   }
}
