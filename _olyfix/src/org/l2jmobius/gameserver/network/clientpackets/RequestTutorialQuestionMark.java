package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPressTutorialMark;
import org.l2jmobius.gameserver.mechanics.script.QuestState;

public class RequestTutorialQuestionMark extends ClientPacket {
   private int _number = 0;

   @Override
   protected void readImpl() {
      this._number = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         QuestState qs = player.getQuestState("Q00255_Tutorial");
         if (qs != null) {
            qs.getQuest().notifyEvent("QM:" + this._number, null, player);
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK, player)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPressTutorialMark(player, this._number), player);
         }
      }
   }
}
