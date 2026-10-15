package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;

public class RequestQuestAbort extends ClientPacket {
   private int _questId;

   @Override
   protected void readImpl() {
      this._questId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Quest qe = ScriptManager.getInstance().getQuest(this._questId);
         if (qe != null) {
            QuestState qs = player.getQuestState(qe.getName());
            if (qs != null) {
               qs.exitQuest(true);
               player.sendPacket(new QuestList(player));
            }
         }
      }
   }
}
