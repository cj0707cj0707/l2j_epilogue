package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExCubeGameChangeTeam extends ServerPacket {
   private final Player _player;
   private final boolean _fromRedTeam;

   public ExCubeGameChangeTeam(Player player, boolean fromRedTeam) {
      this._player = player;
      this._fromRedTeam = fromRedTeam;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_LIST.writeId(this, buffer);
      buffer.writeInt(5);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._fromRedTeam);
      buffer.writeInt(!this._fromRedTeam);
   }
}
