package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExCubeGameRemovePlayer extends ServerPacket {
   private final Player _player;
   private final boolean _isRedTeam;

   public ExCubeGameRemovePlayer(Player player, boolean isRedTeam) {
      this._player = player;
      this._isRedTeam = isRedTeam;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BLOCK_UP_SET_LIST.writeId(this, buffer);
      buffer.writeInt(2);
      buffer.writeInt(-1);
      buffer.writeInt(this._isRedTeam);
      buffer.writeInt(this._player.getObjectId());
   }
}
