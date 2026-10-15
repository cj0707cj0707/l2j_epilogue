package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowVariationMakeWindow extends ServerPacket {
   public static final ExShowVariationMakeWindow STATIC_PACKET = new ExShowVariationMakeWindow();

   private ExShowVariationMakeWindow() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_VARIATION_MAKE_WINDOW.writeId(this, buffer);
   }
}
