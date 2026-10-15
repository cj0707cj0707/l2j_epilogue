package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

public class TutorialCloseHtml extends ServerPacket {
   public static final TutorialCloseHtml STATIC_PACKET = new TutorialCloseHtml();

   private TutorialCloseHtml() {
   }

   @Override
   public void runImpl(Player player) {
      if (player != null) {
         player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.TUTORIAL_CLOSE_HTML.writeId(this, buffer);
   }
}
