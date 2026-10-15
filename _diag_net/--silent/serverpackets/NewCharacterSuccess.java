package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;

public class NewCharacterSuccess extends ServerPacket {
   private final List<PlayerTemplate> _chars = new ArrayList<>();

   public void addChar(PlayerTemplate template) {
      this._chars.add(template);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.NEW_CHARACTER_SUCCESS.writeId(this, buffer);
      buffer.writeInt(this._chars.size());

      for (PlayerTemplate chr : this._chars) {
         if (chr != null) {
            buffer.writeInt(chr.getRace().ordinal());
            buffer.writeInt(chr.getPlayerClass().getId());
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseSTR());
            buffer.writeInt(10);
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseDEX());
            buffer.writeInt(10);
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseCON());
            buffer.writeInt(10);
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseINT());
            buffer.writeInt(10);
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseWIT());
            buffer.writeInt(10);
            buffer.writeInt(70);
            buffer.writeInt(chr.getBaseMEN());
            buffer.writeInt(10);
         }
      }
   }
}
