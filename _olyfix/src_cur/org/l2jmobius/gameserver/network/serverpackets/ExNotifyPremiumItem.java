package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExNotifyPremiumItem extends ServerPacket {
   public static final ExNotifyPremiumItem STATIC_PACKET = new ExNotifyPremiumItem();

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_NOTIFY_PREMIUM_ITEM.writeId(this, buffer);
   }
}
