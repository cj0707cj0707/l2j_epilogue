package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharCreateOk extends ServerPacket {
   public static final CharCreateOk STATIC_PACKET = new CharCreateOk();

   private CharCreateOk() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHARACTER_CREATE_SUCCESS.writeId(this, buffer);
      buffer.writeInt(1);
   }
}
