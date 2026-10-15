package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;

public class RequestGetItemFromPet extends ClientPacket {
   private int _objectId;
   private long _amount;
   private int _unknown;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._amount = this.readLong();
      this._unknown = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (this._amount > 0L && player != null && player.hasPet()) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You get items from pet too fast.");
         } else if (player.getActiveEnchantItemId() != -1) {
            player.setActiveEnchantItemId(-1);
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
            player.sendPacket(new EnchantResult(2, 0, 0));
         } else {
            Pet pet = player.getSummon().asPet();
            Item item = pet.getInventory().getItemByObjectId(this._objectId);
            if (item != null) {
               if (this._amount > item.getCount()) {
                  PunishmentManager.handleIllegalPlayerAction(
                     player,
                     this.getClass().getSimpleName()
                        + ": Character "
                        + player.getName()
                        + " of account "
                        + player.getAccountName()
                        + " tried to get item with oid "
                        + this._objectId
                        + " from pet but has invalid count "
                        + this._amount
                        + " item count: "
                        + item.getCount(),
                     GeneralConfig.DEFAULT_PUNISH
                  );
               } else {
                  if (pet.transferItem(ItemProcessType.TRANSFER, this._objectId, this._amount, player.getInventory(), player, pet) == null) {
                     org.l2jmobius.gameserver.network.PacketLogger.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
                  }
               }
            }
         }
      }
   }
}
