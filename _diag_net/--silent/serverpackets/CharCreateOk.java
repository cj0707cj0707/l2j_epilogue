package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class CharCreateOk extends ServerPacket {
   public static final CharCreateOk STATIC_PACKET = new CharCreateOk();

   private CharCreateOk() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHARACTER_CREATE_SUCCESS.writeId(this, buffer);
      buffer.writeInt(1);
   }
}
