package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class ExFishingStart extends ServerPacket {
   private final Creature _creature;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _fishType;
   private final boolean _isNightLure;

   public ExFishingStart(Creature creature, int fishType, int x, int y, int z, boolean isNightLure) {
      this._creature = creature;
      this._fishType = fishType;
      this._x = x;
      this._y = y;
      this._z = z;
      this._isNightLure = isNightLure;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_FISHING_START.writeId(this, buffer);
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeInt(this._fishType);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeByte(this._isNightLure);
      buffer.writeByte(0);
   }
}
