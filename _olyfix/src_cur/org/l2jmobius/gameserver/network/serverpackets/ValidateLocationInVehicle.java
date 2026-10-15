package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ValidateLocationInVehicle extends ServerPacket {
   private final int _objectId;
   private final int _boatObjId;
   private final int _heading;
   private final Location _pos;

   public ValidateLocationInVehicle(Player player) {
      this._objectId = player.getObjectId();
      this._boatObjId = player.getBoat().getObjectId();
      this._heading = player.getHeading();
      this._pos = player.getInVehiclePosition();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.VALIDATE_LOCATION_IN_VEHICLE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._boatObjId);
      buffer.writeInt(this._pos.getX());
      buffer.writeInt(this._pos.getY());
      buffer.writeInt(this._pos.getZ());
      buffer.writeInt(this._heading);
   }
}
