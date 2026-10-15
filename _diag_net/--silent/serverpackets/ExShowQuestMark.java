package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExShowQuestMark extends ServerPacket {
   private final int _questId;

   public ExShowQuestMark(int questId) {
      this._questId = questId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_QUEST_MARK.writeId(this, buffer);
      buffer.writeInt(this._questId);
   }
}
