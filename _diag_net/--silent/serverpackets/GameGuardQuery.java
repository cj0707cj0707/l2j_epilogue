package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class GameGuardQuery extends ServerPacket {
   public static final GameGuardQuery STATIC_PACKET = new GameGuardQuery();

   private GameGuardQuery() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GAME_GUARD_QUERY.writeId(this, buffer);
      buffer.writeInt(659766745);
      buffer.writeInt(779265309);
      buffer.writeInt(538379147);
      buffer.writeInt(-1017438557);
   }
}
