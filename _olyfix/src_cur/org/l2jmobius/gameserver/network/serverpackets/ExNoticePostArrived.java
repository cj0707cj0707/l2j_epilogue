package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExNoticePostArrived extends ServerPacket {
   private static final ExNoticePostArrived STATIC_PACKET_TRUE = new ExNoticePostArrived(true);
   private static final ExNoticePostArrived STATIC_PACKET_FALSE = new ExNoticePostArrived(false);
   private final boolean _showAnim;

   public static ExNoticePostArrived valueOf(boolean result) {
      return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
   }

   public ExNoticePostArrived(boolean showAnimation) {
      this._showAnim = showAnimation;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_NOTICE_POST_ARRIVED.writeId(this, buffer);
      buffer.writeInt(this._showAnim);
   }
}
