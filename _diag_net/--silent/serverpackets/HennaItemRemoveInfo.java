package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;

public class HennaItemRemoveInfo extends ServerPacket {
   private final Player _player;
   private final Henna _henna;

   public HennaItemRemoveInfo(Henna henna, Player player) {
      this._henna = henna;
      this._player = player;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.HENNA_UNEQUIP_INFO.writeId(this, buffer);
      buffer.writeInt(this._henna.getDyeId());
      buffer.writeInt(this._henna.getDyeItemId());
      buffer.writeLong(this._henna.getCancelCount());
      buffer.writeLong(this._henna.getCancelFee());
      buffer.writeInt(this._henna.isAllowedClass(this._player.getPlayerClass()));
      buffer.writeLong(this._player.getAdena());
      buffer.writeInt(this._player.getINT());
      buffer.writeByte(this._player.getINT() - this._henna.getStatINT());
      buffer.writeInt(this._player.getSTR());
      buffer.writeByte(this._player.getSTR() - this._henna.getStatSTR());
      buffer.writeInt(this._player.getCON());
      buffer.writeByte(this._player.getCON() - this._henna.getStatCON());
      buffer.writeInt(this._player.getMEN());
      buffer.writeByte(this._player.getMEN() - this._henna.getStatMEN());
      buffer.writeInt(this._player.getDEX());
      buffer.writeByte(this._player.getDEX() - this._henna.getStatDEX());
      buffer.writeInt(this._player.getWIT());
      buffer.writeByte(this._player.getWIT() - this._henna.getStatWIT());
   }
}
