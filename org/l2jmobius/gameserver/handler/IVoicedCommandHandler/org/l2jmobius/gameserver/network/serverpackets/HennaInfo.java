package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class HennaInfo extends ServerPacket {
   private final Player _player;
   private final List<Henna> _hennas = new ArrayList<>();

   public HennaInfo(Player player) {
      this._player = player;

      for (Henna henna : this._player.getHennaList()) {
         if (henna != null) {
            this._hennas.add(henna);
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.HENNA_INFO.writeId(this, buffer);
      buffer.writeByte(this._player.getHennaStatINT());
      buffer.writeByte(this._player.getHennaStatSTR());
      buffer.writeByte(this._player.getHennaStatCON());
      buffer.writeByte(this._player.getHennaStatMEN());
      buffer.writeByte(this._player.getHennaStatDEX());
      buffer.writeByte(this._player.getHennaStatWIT());
      buffer.writeInt(3);
      buffer.writeInt(this._hennas.size());

      for (Henna henna : this._hennas) {
         buffer.writeInt(henna.getDyeId());
         buffer.writeInt(1);
      }
   }
}
