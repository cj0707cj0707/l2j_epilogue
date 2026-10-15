package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class TutorialShowQuestionMark extends ServerPacket {
   private final int _markId;

   public TutorialShowQuestionMark(int blink) {
      this._markId = blink;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHOW_TUTORIAL_MARK.writeId(this, buffer);
      buffer.writeInt(this._markId);
   }
}
