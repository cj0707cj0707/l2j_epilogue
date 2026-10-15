package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class StopRotation extends ServerPacket {
   private final int _objectId;
   private final int _degree;
   private final int _speed;

   public StopRotation(int objectId, int degree, int speed) {
      this._objectId = objectId;
      this._degree = degree;
      this._speed = speed;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.FINISH_ROTATING.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._degree);
      buffer.writeInt(this._speed);
      buffer.writeByte(0);
   }
}
