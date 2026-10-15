package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;

public class RequestTutorialLinkHtml extends ClientPacket {
   private String _bypass;

   @Override
   protected void readImpl() {
      this._bypass = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._bypass.equalsIgnoreCase("close")) {
            player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
         }

         IBypassHandler handler = BypassHandler.getInstance().getHandler(this._bypass);
         if (handler != null) {
            handler.onCommand(this._bypass, player, null);
         } else {
            QuestState qs = player.getQuestState("Q00255_Tutorial");
            if (qs != null) {
               qs.getQuest().notifyEvent("TL:" + this._bypass, null, player);
            }
         }
      }
   }
}
