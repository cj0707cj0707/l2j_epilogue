package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.QuestState;

public class RequestTutorialClientEvent extends ClientPacket {
   private int _eventId = 0;

   @Override
   protected void readImpl() {
      this._eventId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         QuestState qs = player.getQuestState("Q00255_Tutorial");
         if (qs != null) {
            qs.getQuest().notifyEvent("CE:" + this._eventId, null, player);
         }
      }
   }
}
