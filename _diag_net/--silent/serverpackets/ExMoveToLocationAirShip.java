package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class ExMoveToLocationAirShip extends ServerPacket {
   private final int _objId;
   private final int _tx;
   private final int _ty;
   private final int _tz;
   private final int _x;
   private final int _y;
   private final int _z;

   public ExMoveToLocationAirShip(Creature creature) {
      this._objId = creature.getObjectId();
      this._tx = creature.getXdestination();
      this._ty = creature.getYdestination();
      this._tz = creature.getZdestination();
      this._x = creature.getX();
      this._y = creature.getY();
      this._z = creature.getZ();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_MOVE_TO_LOCATION_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._tx);
      buffer.writeInt(this._ty);
      buffer.writeInt(this._tz);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
