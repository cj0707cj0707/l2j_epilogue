package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestHennaRemove extends ClientPacket {
   private int _symbolId;

   @Override
   protected void readImpl() {
      this._symbolId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            boolean found = false;

            for (int i = 1; i <= 3; i++) {
               Henna henna = player.getHenna(i);
               if (henna != null && henna.getDyeId() == this._symbolId) {
                  if (player.getAdena() >= henna.getCancelFee()) {
                     player.removeHenna(i);
                  } else {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }

                  found = true;
                  break;
               }
            }

            if (!found) {
               org.l2jmobius.gameserver.network.PacketLogger.warning(
                  this.getClass().getSimpleName() + ": " + player + " requested Henna Draw remove without any henna."
               );
               player.sendPacket(ActionFailed.STATIC_PACKET);
            }
         }
      }
   }
}
