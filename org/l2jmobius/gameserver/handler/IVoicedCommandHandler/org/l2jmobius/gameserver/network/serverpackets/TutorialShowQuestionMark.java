package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class TutorialShowQuestionMark extends ServerPacket {
   private final int _markId;

   public TutorialShowQuestionMark(int blink) {
      this._markId = blink;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SHOW_TUTORIAL_MARK.writeId(this, buffer);
      buffer.writeInt(this._markId);
   }
}
