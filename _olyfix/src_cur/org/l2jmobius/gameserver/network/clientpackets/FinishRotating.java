package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.StopRotation;

public class FinishRotating extends ClientPacket {
   private int _degree;

   @Override
   protected void readImpl() {
      this._degree = this.readInt();
      this.readInt();
   }

   @Override
   protected void runImpl() {
      if (PlayerConfig.ENABLE_KEYBOARD_MOVEMENT) {
         Player player = this.getPlayer();
         if (player != null) {
            if (player.isInAirShip() && player.getAirShip().isCaptain(player)) {
               player.getAirShip().setHeading(this._degree);
               player.getAirShip().broadcastPacket(new StopRotation(player.getAirShip().getObjectId(), this._degree, 0));
            } else {
               player.setHeading(this._degree);
               player.broadcastPacket(new StopRotation(player.getObjectId(), this._degree, 0));
            }
         }
      }
   }
}
