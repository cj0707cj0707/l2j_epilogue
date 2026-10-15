package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class StartRotation extends ServerPacket {
   private final int _objectId;
   private final int _degree;
   private final int _side;
   private final int _speed;

   public StartRotation(int objectId, int degree, int side, int speed) {
      this._objectId = objectId;
      this._degree = degree;
      this._side = side;
      this._speed = speed;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.START_ROTATING.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._degree);
      buffer.writeInt(this._side);
      buffer.writeInt(this._speed);
   }
}
