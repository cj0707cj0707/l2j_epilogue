package org.l2jmobius.gameserver.network.holders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.enums.MessageSenderType;

public class MailMessage {
   private static final int EXPIRATION = 360;
   private static final int COD_EXPIRATION = 12;
   private static final int UNLOAD_ATTACHMENTS_INTERVAL = 900000;
   public static final int DELETED = 0;
   public static final int READED = 1;
   public static final int REJECTED = 2;
   private final int _messageId;
   private final int _senderId;
   private final int _receiverId;
   private final long _expiration;
   private String _senderName = null;
   private String _receiverName = null;
   private final String _subject;
   private final String _content;
   private boolean _unread;
   private boolean _returned;
   private int _sendBySystem;
   private boolean _deletedBySender;
   private boolean _deletedByReceiver;
   private final long _reqAdena;
   private boolean _hasAttachments;
   private Mail _attachments = null;
   private ScheduledFuture<?> _unloadTask = null;

   public MailMessage(ResultSet rset) throws SQLException {
      this._messageId = rset.getInt("messageId");
      this._senderId = rset.getInt("senderId");
      this._receiverId = rset.getInt("receiverId");
      this._subject = rset.getString("subject");
      this._content = rset.getString("content");
      this._expiration = rset.getLong("expiration");
      this._reqAdena = rset.getLong("reqAdena");
      this._hasAttachments = Boolean.parseBoolean(rset.getString("hasAttachments"));
      this._unread = Boolean.parseBoolean(rset.getString("isUnread"));
      this._deletedBySender = Boolean.parseBoolean(rset.getString("isDeletedBySender"));
      this._deletedByReceiver = Boolean.parseBoolean(rset.getString("isDeletedByReceiver"));
      this._sendBySystem = rset.getInt("sendBySystem");
      this._returned = Boolean.parseBoolean(rset.getString("isReturned"));
   }

   public MailMessage(int senderId, int receiverId, boolean isCod, String subject, String text, long reqAdena) {
      this._messageId = IdManager.getInstance().getNextId();
      this._senderId = senderId;
      this._receiverId = receiverId;
      this._subject = subject;
      this._content = text;
      this._expiration = isCod ? System.currentTimeMillis() + 43200000L : System.currentTimeMillis() + 1296000000L;
      this._hasAttachments = false;
      this._unread = true;
      this._deletedBySender = false;
      this._deletedByReceiver = false;
      this._reqAdena = reqAdena;
   }

   public MailMessage(int receiverId, String subject, String content, MessageSenderType sendBySystem) {
      this._messageId = IdManager.getInstance().getNextId();
      this._senderId = -1;
      this._receiverId = receiverId;
      this._subject = subject;
      this._content = content;
      this._expiration = System.currentTimeMillis() + 1296000000L;
      this._reqAdena = 0L;
      this._hasAttachments = false;
      this._unread = true;
      this._deletedBySender = true;
      this._deletedByReceiver = false;
      this._sendBySystem = sendBySystem.ordinal();
      this._returned = false;
   }

   public MailMessage(MailMessage msg) {
      this._messageId = IdManager.getInstance().getNextId();
      this._senderId = msg.getSenderId();
      this._receiverId = msg.getSenderId();
      this._subject = "";
      this._content = "";
      this._expiration = System.currentTimeMillis() + 1296000000L;
      this._unread = true;
      this._deletedBySender = true;
      this._deletedByReceiver = false;
      this._sendBySystem = MessageSenderType.NONE.ordinal();
      this._returned = true;
      this._reqAdena = 0L;
      this._hasAttachments = true;
      this._attachments = msg.getAttachments();
      msg.removeAttachments();
      this._attachments.setNewMessageId(this._messageId);
      this._unloadTask = ThreadPool.schedule(new MailMessage.AttachmentsUnloadTask(this), 900000 + Rnd.get(900000));
   }

   public static PreparedStatement getStatement(MailMessage msg, Connection con) throws SQLException {
      PreparedStatement stmt = con.prepareStatement(
         "INSERT INTO messages (messageId, senderId, receiverId, subject, content, expiration, reqAdena, hasAttachments, isUnread, isDeletedBySender, isDeletedByReceiver, sendBySystem, isReturned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      );
      stmt.setInt(1, msg._messageId);
      stmt.setInt(2, msg._senderId);
      stmt.setInt(3, msg._receiverId);
      stmt.setString(4, msg._subject);
      stmt.setString(5, msg._content);
      stmt.setLong(6, msg._expiration);
      stmt.setLong(7, msg._reqAdena);
      stmt.setString(8, String.valueOf(msg._hasAttachments));
      stmt.setString(9, String.valueOf(msg._unread));
      stmt.setString(10, String.valueOf(msg._deletedBySender));
      stmt.setString(11, String.valueOf(msg._deletedByReceiver));
      stmt.setString(12, String.valueOf(msg._sendBySystem));
      stmt.setString(13, String.valueOf(msg._returned));
      return stmt;
   }

   public int getId() {
      return this._messageId;
   }

   public int getSenderId() {
      return this._senderId;
   }

   public int getReceiverId() {
      return this._receiverId;
   }

   public String getSenderName() {
      if (this._senderName == null) {
         if (this._sendBySystem != 0) {
            return "****";
         }

         this._senderName = CharInfoTable.getInstance().getNameById(this._senderId);
         if (this._senderName == null) {
            this._senderName = "";
         }
      }

      return this._senderName;
   }

   public String getReceiverName() {
      if (this._receiverName == null) {
         this._receiverName = CharInfoTable.getInstance().getNameById(this._receiverId);
         if (this._receiverName == null) {
            this._receiverName = "";
         }
      }

      return this._receiverName;
   }

   public String getSubject() {
      return this._subject;
   }

   public String getContent() {
      return this._content;
   }

   public boolean isLocked() {
      return this._reqAdena > 0L;
   }

   public long getExpiration() {
      return this._expiration;
   }

   public int getExpirationSeconds() {
      return (int)(this._expiration / 1000L);
   }

   public boolean isUnread() {
      return this._unread;
   }

   public void markAsRead() {
      if (this._unread) {
         this._unread = false;
         MailManager.getInstance().markAsReadInDb(this._messageId);
      }
   }

   public boolean isDeletedBySender() {
      return this._deletedBySender;
   }

   public void setDeletedBySender() {
      if (!this._deletedBySender) {
         this._deletedBySender = true;
         if (this._deletedByReceiver) {
            MailManager.getInstance().deleteMessageInDb(this._messageId);
         } else {
            MailManager.getInstance().markAsDeletedBySenderInDb(this._messageId);
         }
      }
   }

   public boolean isDeletedByReceiver() {
      return this._deletedByReceiver;
   }

   public void setDeletedByReceiver() {
      if (!this._deletedByReceiver) {
         this._deletedByReceiver = true;
         if (this._deletedBySender) {
            MailManager.getInstance().deleteMessageInDb(this._messageId);
         } else {
            MailManager.getInstance().markAsDeletedByReceiverInDb(this._messageId);
         }
      }
   }

   public int getSendBySystem() {
      return this._sendBySystem;
   }

   public boolean isReturned() {
      return this._returned;
   }

   public void setReturned(boolean value) {
      this._returned = value;
   }

   public long getReqAdena() {
      return this._reqAdena;
   }

   public synchronized Mail getAttachments() {
      if (!this._hasAttachments) {
         return null;
      } else {
         if (this._attachments == null) {
            this._attachments = new Mail(this._senderId, this._messageId);
            this._attachments.restore();
            this._unloadTask = ThreadPool.schedule(new MailMessage.AttachmentsUnloadTask(this), 900000 + Rnd.get(900000));
         }

         return this._attachments;
      }
   }

   public boolean hasAttachments() {
      return this._hasAttachments;
   }

   public synchronized void removeAttachments() {
      if (this._attachments != null) {
         this._attachments = null;
         this._hasAttachments = false;
         MailManager.getInstance().removeAttachmentsInDb(this._messageId);
         if (this._unloadTask != null) {
            this._unloadTask.cancel(false);
         }
      }
   }

   public synchronized Mail createAttachments() {
      if (!this._hasAttachments && this._attachments == null) {
         this._attachments = new Mail(this._senderId, this._messageId);
         this._hasAttachments = true;
         this._unloadTask = ThreadPool.schedule(new MailMessage.AttachmentsUnloadTask(this), 900000 + Rnd.get(900000));
         return this._attachments;
      } else {
         return null;
      }
   }

   protected synchronized void unloadAttachments() {
      if (this._attachments != null) {
         this._attachments.deleteMe();
         this._attachments = null;
      }
   }

   private static class AttachmentsUnloadTask implements Runnable {
      private MailMessage _msg;

      AttachmentsUnloadTask(MailMessage msg) {
         this._msg = msg;
      }

      @Override
      public void run() {
         if (this._msg != null) {
            this._msg.unloadAttachments();
            this._msg = null;
         }
      }
   }
}
