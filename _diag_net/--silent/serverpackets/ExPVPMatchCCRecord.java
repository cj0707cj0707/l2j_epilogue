package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExPVPMatchCCRecord extends ServerPacket {
   public static final int INITIALIZE = 0;
   public static final int UPDATE = 1;
   public static final int FINISH = 2;
   private final int _state;
   private final Map<Player, Integer> _players;

   public ExPVPMatchCCRecord(int state, Map<Player, Integer> players) {
      this._state = state;
      this._players = players;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PVP_MATCH_CC_RECORD.writeId(this, buffer);
      buffer.writeInt(this._state);
      buffer.writeInt(Math.min(this._players.size(), 25));
      int counter = 0;

      for (Entry<Player, Integer> entry : this._players.entrySet()) {
         if (++counter > 25) {
            break;
         }

         buffer.writeString(entry.getKey().getName());
         buffer.writeInt(entry.getValue());
      }
   }
}
