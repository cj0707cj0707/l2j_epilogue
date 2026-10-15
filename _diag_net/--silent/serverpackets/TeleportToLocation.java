package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;

public class TeleportToLocation extends ServerPacket {
   private final int _targetObjId;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;

   public TeleportToLocation(WorldObject obj, int x, int y, int z, int heading) {
      this._targetObjId = obj.getObjectId();
      this._x = x;
      this._y = y;
      this._z = z;
      this._heading = heading;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.TELEPORT_TO_LOCATION.writeId(this, buffer);
      buffer.writeInt(this._targetObjId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(0);
      buffer.writeInt(this._heading);
   }
}
