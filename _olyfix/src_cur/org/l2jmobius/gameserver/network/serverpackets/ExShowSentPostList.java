package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.MailMessage;

public class ExShowSentPostList extends ServerPacket {
   private final List<MailMessage> _outbox;

   public ExShowSentPostList(int objectId) {
      this._outbox = MailManager.getInstance().getOutbox(objectId);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_SENT_POST_LIST.writeId(this, buffer);
      buffer.writeInt((int)(System.currentTimeMillis() / 1000L));
      if (this._outbox != null && !this._outbox.isEmpty()) {
         buffer.writeInt(this._outbox.size());

         for (MailMessage msg : this._outbox) {
            buffer.writeInt(msg.getId());
            buffer.writeString(msg.getSubject());
            buffer.writeString(msg.getReceiverName());
            buffer.writeInt(msg.isLocked());
            buffer.writeInt(msg.getExpirationSeconds());
            buffer.writeInt(msg.isUnread());
            buffer.writeInt(1);
            buffer.writeInt(msg.hasAttachments());
         }
      } else {
         buffer.writeInt(0);
      }
   }
}
