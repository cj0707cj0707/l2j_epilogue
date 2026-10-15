package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.MercTicketManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestPetGetItem extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.hasPet()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            Item item = (Item)World.findObject(this._objectId);
            if (item == null) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               int castleId = MercTicketManager.getInstance().getTicketCastleId(item.getId());
               if (castleId > 0) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (FortSiegeManager.getInstance().isCombat(item.getId())) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  Pet pet = player.getSummon().asPet();
                  if (pet.isDead() || pet.isOutOfControl()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  } else if (pet.isUncontrollable()) {
                     player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
                  } else {
                     pet.getAI().setIntentionPickUp(item);
                  }
               }
            }
         }
      }
   }
}
