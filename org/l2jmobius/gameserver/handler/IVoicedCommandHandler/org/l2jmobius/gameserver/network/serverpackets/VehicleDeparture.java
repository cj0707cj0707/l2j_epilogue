package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class VehicleDeparture extends ServerPacket {
   private final int _objId;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _moveSpeed;
   private final int _rotationSpeed;

   public VehicleDeparture(Boat boat) {
      this._objId = boat.getObjectId();
      this._x = boat.getXdestination();
      this._y = boat.getYdestination();
      this._z = boat.getZdestination();
      this._moveSpeed = (int)boat.getMoveSpeed();
      this._rotationSpeed = (int)boat.getStat().getRotationSpeed();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.VEHICLE_DEPARTURE.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._moveSpeed);
      buffer.writeInt(this._rotationSpeed);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
