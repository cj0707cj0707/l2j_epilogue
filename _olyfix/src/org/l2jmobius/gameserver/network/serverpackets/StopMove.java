package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class StopMove extends ServerPacket {
   private final int _objectId;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;

   public StopMove(Creature creature) {
      this(creature.getObjectId(), creature.getX(), creature.getY(), creature.getZ(), creature.getHeading());
   }

   public StopMove(int objectId, int x, int y, int z, int heading) {
      this._objectId = objectId;
      this._x = x;
      this._y = y;
      this._z = z;
      this._heading = heading;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.STOP_MOVE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._heading);
   }
}
