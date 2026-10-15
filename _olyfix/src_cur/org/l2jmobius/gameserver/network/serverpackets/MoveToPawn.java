package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MoveToPawn extends ServerPacket {
   private final int _objectId;
   private final int _targetId;
   private final int _distance;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _tx;
   private final int _ty;
   private final int _tz;

   public MoveToPawn(Creature creature, WorldObject target, int distance) {
      this._objectId = creature.getObjectId();
      this._targetId = target.getObjectId();
      this._distance = distance;
      this._x = creature.getX();
      this._y = creature.getY();
      this._z = creature.getZ();
      this._tx = target.getX();
      this._ty = target.getY();
      this._tz = target.getZ();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MOVE_TO_PAWN.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._targetId);
      buffer.writeInt(this._distance);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._tx);
      buffer.writeInt(this._ty);
      buffer.writeInt(this._tz);
   }

   public boolean canBeDropped(GameClient client) {
      return true;
   }
}
