package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GetOnVehicle extends ServerPacket {
   private final int _objectId;
   private final int _boatObjId;
   private final Location _pos;

   public GetOnVehicle(int charObjId, int boatObjId, Location pos) {
      this._objectId = charObjId;
      this._boatObjId = boatObjId;
      this._pos = pos;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.GET_ON_VEHICLE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._boatObjId);
      buffer.writeInt(this._pos.getX());
      buffer.writeInt(this._pos.getY());
      buffer.writeInt(this._pos.getZ());
   }
}
