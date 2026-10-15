package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExCubeGameExtendedChangePoints extends ServerPacket {
   private final int _timeLeft;
   private final int _bluePoints;
   private final int _redPoints;
   private final boolean _isRedTeam;
   private final Player _player;
   private final int _playerPoints;

   public ExCubeGameExtendedChangePoints(int timeLeft, int bluePoints, int redPoints, boolean isRedTeam, Player player, int playerPoints) {
      this._timeLeft = timeLeft;
      this._bluePoints = bluePoints;
      this._redPoints = redPoints;
      this._isRedTeam = isRedTeam;
      this._player = player;
      this._playerPoints = playerPoints;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BLOCK_UP_SET_STATE.writeId(this, buffer);
      buffer.writeInt(0);
      buffer.writeInt(this._timeLeft);
      buffer.writeInt(this._bluePoints);
      buffer.writeInt(this._redPoints);
      buffer.writeInt(this._isRedTeam);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._playerPoints);
   }
}
