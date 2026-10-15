package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ShowPCCafeCouponShowUI extends ServerPacket {
   public static final ShowPCCafeCouponShowUI STATIC_PACKET = new ShowPCCafeCouponShowUI();

   private ShowPCCafeCouponShowUI() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHOW_PC_CAFE_COUPON_SHOW_UI.writeId(this, buffer);
   }
}
