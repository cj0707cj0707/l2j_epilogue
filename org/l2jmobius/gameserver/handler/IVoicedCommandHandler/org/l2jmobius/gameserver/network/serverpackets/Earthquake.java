package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class Earthquake extends ServerPacket {
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _intensity;
   private final int _duration;

   public Earthquake(int x, int y, int z, int intensity, int duration) {
      this._x = x;
      this._y = y;
      this._z = z;
      this._intensity = intensity;
      this._duration = duration;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EARTHQUAKE.writeId(this, buffer);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._intensity);
      buffer.writeInt(this._duration);
      buffer.writeInt(0);
   }
}
