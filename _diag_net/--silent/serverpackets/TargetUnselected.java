package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class TargetUnselected extends ServerPacket {
   private final int _targetObjId;
   private final int _x;
   private final int _y;
   private final int _z;

   public TargetUnselected(Creature creature) {
      this._targetObjId = creature.getObjectId();
      this._x = creature.getX();
      this._y = creature.getY();
      this._z = creature.getZ();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.TARGET_UNSELECTED.writeId(this, buffer);
      buffer.writeInt(this._targetObjId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(0);
   }
}
