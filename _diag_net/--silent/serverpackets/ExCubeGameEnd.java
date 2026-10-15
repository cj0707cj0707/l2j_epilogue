package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExCubeGameEnd extends ServerPacket {
   private final boolean _isRedTeamWin;

   public ExCubeGameEnd(boolean isRedTeamWin) {
      this._isRedTeamWin = isRedTeamWin;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_STATE.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._isRedTeamWin);
   }
}
