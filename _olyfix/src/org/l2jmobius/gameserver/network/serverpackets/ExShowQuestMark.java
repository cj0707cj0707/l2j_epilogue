package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowQuestMark extends ServerPacket {
   private final int _questId;

   public ExShowQuestMark(int questId) {
      this._questId = questId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_QUEST_MARK.writeId(this, buffer);
      buffer.writeInt(this._questId);
   }
}
