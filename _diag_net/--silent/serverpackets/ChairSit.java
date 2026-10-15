package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ChairSit extends ServerPacket {
   private final Player _player;
   private final int _staticObjectId;

   public ChairSit(Player player, int staticObjectId) {
      this._player = player;
      this._staticObjectId = staticObjectId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHAIR_SIT.writeId(this, buffer);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._staticObjectId);
   }
}
