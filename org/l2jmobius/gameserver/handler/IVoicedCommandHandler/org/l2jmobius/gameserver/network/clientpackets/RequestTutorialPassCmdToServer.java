package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.script.QuestState;

public class RequestTutorialPassCmdToServer extends ClientPacket {
   private String _bypass = null;

   @Override
   protected void readImpl() {
      this._bypass = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         IBypassHandler handler = BypassHandler.getInstance().getHandler(this._bypass);
         if (handler != null) {
            handler.onCommand(this._bypass, player, null);
         } else {
            QuestState qs = player.getQuestState("Q00255_Tutorial");
            if (qs != null) {
               qs.getQuest().notifyEvent("TC:" + this._bypass, null, player);
            }
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_BYPASS, player)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerBypass(player, this._bypass), player);
         }
      }
   }
}
