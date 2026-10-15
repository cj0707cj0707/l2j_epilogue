package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.BoatManager;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;

public class RequestGetOnVehicle extends ClientPacket {
   private int _boatId;
   private Location _pos;

   @Override
   protected void readImpl() {
      this._boatId = this.readInt();
      this._pos = new Location(this.readInt(), this.readInt(), this.readInt());
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Boat boat;
         if (player.isInBoat()) {
            boat = player.getBoat();
            if (boat.getObjectId() != this._boatId) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
               return;
            }
         } else {
            boat = BoatManager.getInstance().getBoat(this._boatId);
            if (boat == null || boat.isMoving() || !player.isInsideRadius3D(boat, 1000)) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
               return;
            }
         }

         player.setInVehiclePosition(this._pos);
         player.setVehicle(boat);
         player.broadcastPacket(new GetOnVehicle(player.getObjectId(), boat.getObjectId(), this._pos));
         player.setXYZ(boat.getX(), boat.getY(), boat.getZ());
         player.setInsideZone(ZoneId.PEACE, true);
         player.revalidateZone(true);
      }
   }
}
