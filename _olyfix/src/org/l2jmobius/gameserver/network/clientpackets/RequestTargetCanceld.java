package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.TargetUnselected;

public class RequestTargetCanceld extends ClientPacket {
   private int _unselect;

   @Override
   protected void readImpl() {
      this._unselect = this.readShort();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isLockedTarget()) {
            player.sendPacket(SystemMessageId.FAILED_TO_DISABLE_ATTACK_TARGET);
         } else {
            if (this._unselect == 0) {
               if (player.isCastingNow() && player.canAbortCast()) {
                  player.abortCast();
               } else if (player.getTarget() != null) {
                  player.setTarget(null);
               }
            } else if (player.getTarget() != null) {
               player.setTarget(null);
            } else if (player.isInAirShip()) {
               player.broadcastPacket(new TargetUnselected(player));
            }
         }
      }
   }
}
