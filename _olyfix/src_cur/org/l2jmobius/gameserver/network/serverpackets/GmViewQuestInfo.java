package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GmViewQuestInfo extends ServerPacket {
   private final Player _player;

   public GmViewQuestInfo(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.GM_VIEW_QUEST_INFO.writeId(this, buffer);
      buffer.writeString(this._player.getName());
      List<Quest> questList = this._player.getAllActiveQuests();
      if (questList.isEmpty()) {
         buffer.writeByte(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
      } else {
         buffer.writeShort(questList.size());

         for (Quest q : questList) {
            buffer.writeInt(q.getId());
            QuestState qs = this._player.getQuestState(q.getName());
            if (qs == null) {
               buffer.writeInt(0);
            } else {
               buffer.writeInt(qs.getCond());
            }
         }
      }
   }
}
