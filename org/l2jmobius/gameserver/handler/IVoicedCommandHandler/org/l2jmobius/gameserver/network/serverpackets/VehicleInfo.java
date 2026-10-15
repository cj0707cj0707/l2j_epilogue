package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class VehicleInfo extends ServerPacket {
   private final int _objId;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;

   public VehicleInfo(Boat boat) {
      this._objId = boat.getObjectId();
      this._x = boat.getX();
      this._y = boat.getY();
      this._z = boat.getZ();
      this._heading = boat.getHeading();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.VEHICLE_INFO.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._heading);
   }
}
