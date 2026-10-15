package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.MailMessage;

public class ExShowReceivedPostList extends ServerPacket {
   private final List<MailMessage> _inbox;

   public ExShowReceivedPostList(int objectId) {
      this._inbox = MailManager.getInstance().getInbox(objectId);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_RECEIVED_POST_LIST.writeId(this, buffer);
      buffer.writeInt((int)(System.currentTimeMillis() / 1000L));
      if (this._inbox != null && !this._inbox.isEmpty()) {
         buffer.writeInt(this._inbox.size());

         for (MailMessage msg : this._inbox) {
            buffer.writeInt(msg.getId());
            buffer.writeString(msg.getSubject());
            buffer.writeString(msg.getSenderName());
            buffer.writeInt(msg.isLocked());
            buffer.writeInt(msg.getExpirationSeconds());
            buffer.writeInt(msg.isUnread());
            buffer.writeInt(1);
            buffer.writeInt(msg.hasAttachments());
            buffer.writeInt(msg.getSendBySystem());
            buffer.writeInt(msg.isReturned());
         }
      } else {
         buffer.writeInt(0);
      }
   }
}
