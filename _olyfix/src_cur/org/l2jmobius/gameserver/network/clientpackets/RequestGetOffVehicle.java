package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.GetOffVehicle;
import org.l2jmobius.gameserver.network.serverpackets.StopMoveInVehicle;

public class RequestGetOffVehicle extends ClientPacket {
   private int _boatId;
   private int _x;
   private int _y;
   private int _z;

   @Override
   protected void readImpl() {
      this._boatId = this.readInt();
      this._x = this.readInt();
      this._y = this.readInt();
      this._z = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isInBoat()
            && player.getBoat().getObjectId() == this._boatId
            && !player.getBoat().isMoving()
            && player.isInsideRadius3D(this._x, this._y, this._z, 1000)) {
            player.broadcastPacket(new StopMoveInVehicle(player, this._boatId));
            player.setVehicle(null);
            player.setInVehiclePosition(null);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            player.broadcastPacket(new GetOffVehicle(player.getObjectId(), this._boatId, this._x, this._y, this._z));
            player.setXYZ(this._x, this._y, this._z);
            player.setInsideZone(ZoneId.PEACE, false);
            player.revalidateZone(true);
         } else {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }
}
