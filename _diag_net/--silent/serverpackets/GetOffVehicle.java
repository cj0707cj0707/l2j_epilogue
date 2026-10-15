package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class GetOffVehicle extends ServerPacket {
   private final int _objectId;
   private final int _boatObjId;
   private final int _x;
   private final int _y;
   private final int _z;

   public GetOffVehicle(int charObjId, int boatObjId, int x, int y, int z) {
      this._objectId = charObjId;
      this._boatObjId = boatObjId;
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GET_OFF_VEHICLE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._boatObjId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
