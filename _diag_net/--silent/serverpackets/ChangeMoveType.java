package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class ChangeMoveType extends ServerPacket {
   public static final int WALK = 0;
   public static final int RUN = 1;
   private final int _objectId;
   private final boolean _running;

   public ChangeMoveType(Creature creature) {
      this._objectId = creature.getObjectId();
      this._running = creature.isRunning();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHANGE_MOVE_TYPE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._running ? 1 : 0);
      buffer.writeInt(0);
   }
}
