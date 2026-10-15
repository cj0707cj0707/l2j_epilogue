package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExDuelUpdateUserInfo extends ServerPacket {
   private final Player _player;

   public ExDuelUpdateUserInfo(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_DUEL_UPDATE_USER_INFO.writeId(this, buffer);
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(this._player.getLevel());
      buffer.writeInt((int)this._player.getCurrentHp());
      buffer.writeInt(this._player.getMaxHp());
      buffer.writeInt((int)this._player.getCurrentMp());
      buffer.writeInt(this._player.getMaxMp());
      buffer.writeInt((int)this._player.getCurrentCp());
      buffer.writeInt(this._player.getMaxCp());
   }
}
