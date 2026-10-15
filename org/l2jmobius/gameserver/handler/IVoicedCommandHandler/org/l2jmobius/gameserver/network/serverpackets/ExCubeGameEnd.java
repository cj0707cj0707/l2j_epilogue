package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExCubeGameEnd extends ServerPacket {
   private final boolean _isRedTeamWin;

   public ExCubeGameEnd(boolean isRedTeamWin) {
      this._isRedTeamWin = isRedTeamWin;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BLOCK_UP_SET_STATE.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._isRedTeamWin);
   }
}
