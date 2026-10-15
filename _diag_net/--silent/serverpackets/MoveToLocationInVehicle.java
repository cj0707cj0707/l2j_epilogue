package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

public class MoveToLocationInVehicle extends ServerPacket {
   private final int _objectId;
   private final int _boatId;
   private final Location _destination;
   private final Location _origin;

   public MoveToLocationInVehicle(Player player, Location destination, Location origin) {
      this._objectId = player.getObjectId();
      this._boatId = player.getBoat().getObjectId();
      this._destination = destination;
      this._origin = origin;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.MOVE_TO_LOCATION_IN_VEHICLE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._boatId);
      buffer.writeInt(this._destination.getX());
      buffer.writeInt(this._destination.getY());
      buffer.writeInt(this._destination.getZ());
      buffer.writeInt(this._origin.getX());
      buffer.writeInt(this._origin.getY());
      buffer.writeInt(this._origin.getZ());
   }
}
