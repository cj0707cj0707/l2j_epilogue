package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExOlympiadUserInfo extends ServerPacket {
   private final Player _player;
   private final int _side;

   public ExOlympiadUserInfo(Player player, int side) {
      this._player = player;
      this._side = side;
   }

   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_OLYMPIAD_USER_INFO.writeId(this, buffer);
      buffer.writeByte(this._side);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt((int)this._player.getCurrentHp());
      buffer.writeInt(this._player.getMaxHp());
      buffer.writeInt((int)this._player.getCurrentCp());
      buffer.writeInt(this._player.getMaxCp());
   }
}
