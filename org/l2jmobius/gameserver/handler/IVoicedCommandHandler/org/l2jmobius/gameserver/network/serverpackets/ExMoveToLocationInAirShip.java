package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExMoveToLocationInAirShip extends ServerPacket {
   private final int _objectId;
   private final int _airShipId;
   private final Location _destination;
   private final int _heading;

   public ExMoveToLocationInAirShip(Player player) {
      this._objectId = player.getObjectId();
      this._airShipId = player.getAirShip().getObjectId();
      this._destination = player.getInVehiclePosition();
      this._heading = player.getHeading();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_MOVE_TO_LOCATION_IN_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._airShipId);
      buffer.writeInt(this._destination.getX());
      buffer.writeInt(this._destination.getY());
      buffer.writeInt(this._destination.getZ());
      buffer.writeInt(this._heading);
   }
}
