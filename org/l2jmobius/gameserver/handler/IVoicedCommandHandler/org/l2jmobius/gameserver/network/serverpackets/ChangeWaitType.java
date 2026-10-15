package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ChangeWaitType extends ServerPacket {
   public static final int WT_SITTING = 0;
   public static final int WT_STANDING = 1;
   public static final int WT_START_FAKEDEATH = 2;
   public static final int WT_STOP_FAKEDEATH = 3;
   private final int _objectId;
   private final int _moveType;
   private final int _x;
   private final int _y;
   private final int _z;

   public ChangeWaitType(Creature creature, int newMoveType) {
      this._objectId = creature.getObjectId();
      this._moveType = newMoveType;
      this._x = creature.getX();
      this._y = creature.getY();
      this._z = creature.getZ();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHANGE_WAIT_TYPE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._moveType);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
