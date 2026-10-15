package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

public class NpcQuestHtmlMessage extends AbstractHtmlPacket {
   private final int _questId;

   public NpcQuestHtmlMessage(int npcObjId, int questId) {
      super(npcObjId);
      this._questId = questId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_NPC_QUEST_HTML_MESSAGE.writeId(this, buffer);
      buffer.writeInt(this.getNpcObjId());
      buffer.writeString(this.getHtml());
      buffer.writeInt(this._questId);
   }

   @Override
   public HtmlActionScope getScope() {
      return HtmlActionScope.NPC_QUEST_HTML;
   }
}
