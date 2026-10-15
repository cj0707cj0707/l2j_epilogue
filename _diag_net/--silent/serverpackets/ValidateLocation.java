package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;

public class ValidateLocation extends ServerPacket {
   private final int _objectId;
   private final Location _loc;

   public ValidateLocation(WorldObject obj) {
      this._objectId = obj.getObjectId();
      this._loc = obj.getLocation();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.VALIDATE_LOCATION.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._loc.getX());
      buffer.writeInt(this._loc.getY());
      buffer.writeInt(this._loc.getZ());
      buffer.writeInt(this._loc.getHeading());
   }
}
