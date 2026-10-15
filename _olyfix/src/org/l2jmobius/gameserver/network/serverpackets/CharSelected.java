package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class CharSelected extends ServerPacket {
   private final Player _player;
   private final int _sessionId;

   public CharSelected(Player player, int sessionId) {
      this._player = player;
      this._sessionId = sessionId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHARACTER_SELECTED.writeId(this, buffer);
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._player.getObjectId());
      buffer.writeString(this._player.getTitle());
      buffer.writeInt(this._sessionId);
      buffer.writeInt(this._player.getClanId());
      buffer.writeInt(0);
      buffer.writeInt(this._player.getAppearance().isFemale());
      buffer.writeInt(this._player.getRace().ordinal());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(1);
      buffer.writeInt(this._player.getX());
      buffer.writeInt(this._player.getY());
      buffer.writeInt(this._player.getZ());
      buffer.writeDouble(this._player.getCurrentHp());
      buffer.writeDouble(this._player.getCurrentMp());
      buffer.writeInt((int)this._player.getSp());
      buffer.writeLong(this._player.getExp());
      buffer.writeInt(this._player.getLevel());
      buffer.writeInt(this._player.getKarma());
      buffer.writeInt(this._player.getPkKills());
      buffer.writeInt(this._player.getINT());
      buffer.writeInt(this._player.getSTR());
      buffer.writeInt(this._player.getCON());
      buffer.writeInt(this._player.getMEN());
      buffer.writeInt(this._player.getDEX());
      buffer.writeInt(this._player.getWIT());
      buffer.writeInt(GameTimeTaskManager.getInstance().getGameTime() % 1440);
      buffer.writeInt(0);
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeBytes();
      buffer.writeInt(0);
   }
}
