package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExNotifyPremiumItem extends ServerPacket {
   public static final ExNotifyPremiumItem STATIC_PACKET = new ExNotifyPremiumItem();

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_NOTIFY_PREMIUM_ITEM.writeId(this, buffer);
   }
}
