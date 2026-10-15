package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class QuestList extends ServerPacket {
   final Player _player;

   public QuestList(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      List<Quest> quests = this._player.getAllActiveQuests();
      ServerPackets.QUEST_LIST.writeId(this, buffer);
      buffer.writeShort(quests.size());

      for (Quest q : quests) {
         buffer.writeInt(q.getId());
         QuestState qs = this._player.getQuestState(q.getName());
         if (qs == null) {
            buffer.writeInt(0);
         } else {
            int states = qs.getInt("__compltdStateFlags");
            if (states > 0) {
               buffer.writeInt(states);
            } else {
               buffer.writeInt(qs.getCond());
            }
         }
      }

      buffer.writeBytes();
   }
}
