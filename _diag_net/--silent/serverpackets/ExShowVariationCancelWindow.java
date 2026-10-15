package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExShowVariationCancelWindow extends ServerPacket {
   public static final ExShowVariationCancelWindow STATIC_PACKET = new ExShowVariationCancelWindow();

   private ExShowVariationCancelWindow() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_VARIATION_CANCEL_WINDOW.writeId(this, buffer);
   }
}
