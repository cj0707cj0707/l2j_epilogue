package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

public class TutorialShowHtml extends AbstractHtmlPacket {
   public TutorialShowHtml(String html) {
      super(html);
   }

   public TutorialShowHtml(int npcObjId, String html) {
      super(npcObjId, html);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TUTORIAL_SHOW_HTML.writeId(this, buffer);
      buffer.writeString(this.getHtml());
   }

   @Override
   public HtmlActionScope getScope() {
      return HtmlActionScope.TUTORIAL_HTML;
   }
}
