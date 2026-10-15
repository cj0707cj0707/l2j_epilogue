package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExVitalityPointInfo extends ServerPacket {
   private final int _vitalityPoints;

   public ExVitalityPointInfo(int vitPoints) {
      this._vitalityPoints = vitPoints;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_VITALITY_POINT_INFO.writeId(this, buffer);
      buffer.writeInt(this._vitalityPoints);
   }
}
