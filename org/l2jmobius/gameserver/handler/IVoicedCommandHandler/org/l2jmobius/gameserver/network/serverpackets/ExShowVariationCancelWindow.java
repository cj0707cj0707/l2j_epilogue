package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowVariationCancelWindow extends ServerPacket {
   public static final ExShowVariationCancelWindow STATIC_PACKET = new ExShowVariationCancelWindow();

   private ExShowVariationCancelWindow() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_VARIATION_CANCEL_WINDOW.writeId(this, buffer);
   }
}
