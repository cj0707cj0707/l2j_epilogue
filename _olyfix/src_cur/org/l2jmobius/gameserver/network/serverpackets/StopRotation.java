package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

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
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.FINISH_ROTATING.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._degree);
      buffer.writeInt(this._speed);
      buffer.writeByte(0);
   }
}
