package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ShortBuffStatusUpdate extends ServerPacket {
   public static final ShortBuffStatusUpdate RESET_SHORT_BUFF = new ShortBuffStatusUpdate(0, 0, 0);
   private final int _skillId;
   private final int _skillLevel;
   private final int _duration;

   public ShortBuffStatusUpdate(int skillId, int skillLevel, int duration) {
      this._skillId = skillId;
      this._skillLevel = skillLevel;
      this._duration = duration;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHORT_BUFF_STATUS_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._skillId);
      buffer.writeInt(this._skillLevel);
      buffer.writeInt(this._duration);
   }
}
