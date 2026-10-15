package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.StopMoveInVehicle;

public class CannotMoveAnymoreInVehicle extends ClientPacket {
   private int _x;
   private int _y;
   private int _z;
   private int _heading;
   private int _boatId;

   @Override
   protected void readImpl() {
      this._boatId = this.readInt();
      this._x = this.readInt();
      this._y = this.readInt();
      this._z = this.readInt();
      this._heading = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isInBoat() && player.getBoat().getObjectId() == this._boatId) {
            player.setInVehiclePosition(new Location(this._x, this._y, this._z));
            player.setHeading(this._heading);
            player.broadcastPacket(new StopMoveInVehicle(player, this._boatId));
         }
      }
   }
}
