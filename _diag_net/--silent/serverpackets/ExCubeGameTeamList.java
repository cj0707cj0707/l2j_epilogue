package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExCubeGameTeamList extends ServerPacket {
   private final List<Player> _bluePlayers;
   private final List<Player> _redPlayers;
   private final int _roomNumber;

   public ExCubeGameTeamList(List<Player> redPlayers, List<Player> bluePlayers, int roomNumber) {
      this._redPlayers = redPlayers;
      this._bluePlayers = bluePlayers;
      this._roomNumber = roomNumber - 1;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_LIST.writeId(this, buffer);
      buffer.writeInt(0);
      buffer.writeInt(this._roomNumber);
      buffer.writeInt(-1);
      buffer.writeInt(this._bluePlayers.size());

      for (Player player : this._bluePlayers) {
         buffer.writeInt(player.getObjectId());
         buffer.writeString(player.getName());
      }

      buffer.writeInt(this._redPlayers.size());

      for (Player player : this._redPlayers) {
         buffer.writeInt(player.getObjectId());
         buffer.writeString(player.getName());
      }
   }
}
