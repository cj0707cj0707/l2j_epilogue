package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class VehicleCheckLocation extends ServerPacket {
   private final Creature _boat;

   public VehicleCheckLocation(Creature boat) {
      this._boat = boat;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.VEHICLE_CHECK_LOCATION.writeId(this, buffer);
      buffer.writeInt(this._boat.getObjectId());
      buffer.writeInt(this._boat.getX());
      buffer.writeInt(this._boat.getY());
      buffer.writeInt(this._boat.getZ());
      buffer.writeInt(this._boat.getHeading());
   }
}
