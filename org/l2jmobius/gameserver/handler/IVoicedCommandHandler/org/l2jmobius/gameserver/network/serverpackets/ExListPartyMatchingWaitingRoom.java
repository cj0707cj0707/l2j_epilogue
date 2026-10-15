package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExListPartyMatchingWaitingRoom extends ServerPacket {
   private final Player _player;
   private final int _minLevel;
   private final int _maxLevel;
   private final int _mode;
   private final List<Player> _members;

   public ExListPartyMatchingWaitingRoom(Player player, int page, int minLevel, int maxLevel, int mode) {
      this._player = player;
      this._minLevel = minLevel;
      this._maxLevel = maxLevel;
      this._mode = mode;
      this._members = new ArrayList<>();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_LIST_PARTY_MATCHING_WAITING_ROOM.writeId(this, buffer);
      if (this._mode == 0) {
         buffer.writeInt(0);
         buffer.writeInt(0);
      } else {
         for (Player cha : PartyMatchWaitingList.getInstance().getPlayers()) {
            if (cha != null && cha != this._player) {
               if (!cha.isPartyWaiting()) {
                  PartyMatchWaitingList.getInstance().removePlayer(cha);
               } else if (cha.getLevel() >= this._minLevel && cha.getLevel() <= this._maxLevel) {
                  this._members.add(cha);
               }
            }
         }

         buffer.writeInt(1);
         buffer.writeInt(this._members.size());

         for (Player member : this._members) {
            buffer.writeString(member.getName());
            buffer.writeInt(member.getActiveClass());
            buffer.writeInt(member.getLevel());
         }
      }
   }
}
