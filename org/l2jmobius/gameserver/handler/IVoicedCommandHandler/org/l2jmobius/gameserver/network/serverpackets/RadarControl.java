package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class RadarControl extends ServerPacket {
   private final int _showRadar;
   private final int _type;
   private final int _x;
   private final int _y;
   private final int _z;

   public RadarControl(int showRadar, int type, int x, int y, int z) {
      this._showRadar = showRadar;
      this._type = type;
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.RADAR_CONTROL.writeId(this, buffer);
      buffer.writeInt(this._showRadar);
      buffer.writeInt(this._type);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
