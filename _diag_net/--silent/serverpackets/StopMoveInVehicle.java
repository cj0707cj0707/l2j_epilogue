package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

public class StopMoveInVehicle extends ServerPacket {
   private final int _objectId;
   private final int _boatId;
   private final Location _pos;
   private final int _heading;

   public StopMoveInVehicle(Player player, int boatId) {
      this._objectId = player.getObjectId();
      this._boatId = boatId;
      this._pos = player.getInVehiclePosition();
      this._heading = player.getHeading();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.STOP_MOVE_IN_VEHICLE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._boatId);
      buffer.writeInt(this._pos.getX());
      buffer.writeInt(this._pos.getY());
      buffer.writeInt(this._pos.getZ());
      buffer.writeInt(this._heading);
   }
}
