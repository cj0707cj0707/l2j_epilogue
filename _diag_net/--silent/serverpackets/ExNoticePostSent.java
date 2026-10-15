package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExNoticePostSent extends ServerPacket {
   private static final ExNoticePostSent STATIC_PACKET_TRUE = new ExNoticePostSent(true);
   private static final ExNoticePostSent STATIC_PACKET_FALSE = new ExNoticePostSent(false);
   private final boolean _showAnim;

   public static ExNoticePostSent valueOf(boolean result) {
      return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
   }

   public ExNoticePostSent(boolean showAnimation) {
      this._showAnim = showAnimation;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_NOTICE_POST_SENT.writeId(this, buffer);
      buffer.writeInt(this._showAnim);
   }
}
