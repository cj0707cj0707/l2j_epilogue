package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SetupGauge extends ServerPacket {
   public static final int BLUE = 0;
   public static final int RED = 1;
   public static final int CYAN = 2;
   private final int _objectId;
   private final int _dat1;
   private final int _time;
   private final int _time2;

   public SetupGauge(int charObjId, int dat1, int time) {
      this._objectId = charObjId;
      this._dat1 = dat1;
      this._time = time;
      this._time2 = time;
   }

   public SetupGauge(int charObjId, int color, int currentTime, int maxTime) {
      this._objectId = charObjId;
      this._dat1 = color;
      this._time = currentTime;
      this._time2 = maxTime;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SETUP_GAUGE.writeId(this, buffer);
      buffer.writeInt(this._dat1);
      buffer.writeInt(this._time);
      buffer.writeInt(this._time2);
   }
}
