package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExCubeGameAddPlayer extends ServerPacket {
   private final Player _player;
   private final boolean _isRedTeam;

   public ExCubeGameAddPlayer(Player player, boolean isRedTeam) {
      this._player = player;
      this._isRedTeam = isRedTeam;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_LIST.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(-1);
      buffer.writeInt(this._isRedTeam);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeString(this._player.getName());
   }
}
