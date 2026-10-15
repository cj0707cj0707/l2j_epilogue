package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jmobius.gameserver.taskmanagers.MessageDeletionTaskManager;

public class MailManager {
   private static final Logger LOGGER = Logger.getLogger(MailManager.class.getName());
   private final Map<Integer, MailMessage> _messages = new ConcurrentHashMap<>();

   protected MailManager() {
      this.load();
   }

   private void load() {
      int count = 0;

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement ps = con.createStatement();
         ResultSet rs = ps.executeQuery("SELECT * FROM messages ORDER BY expiration");
      ) {
         while (rs.next()) {
            count++;
            MailMessage msg = new MailMessage(rs);
            int msgId = msg.getId();
            this._messages.put(msgId, msg);
            MessageDeletionTaskManager.getInstance().add(msgId, msg.getExpiration());
         }
      } catch (SQLException var13) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error loading from database:", (Throwable)var13);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + count + " messages.");
   }

   public MailMessage getMessage(int msgId) {
      return this._messages.get(msgId);
   }

   public Collection<MailMessage> getMessages() {
      return this._messages.values();
   }

   public boolean hasUnreadPost(Player player) {
      int objectId = player.getObjectId();

      for (MailMessage msg : this._messages.values()) {
         if (msg != null && msg.getReceiverId() == objectId && msg.isUnread()) {
            return true;
         }
      }

      return false;
   }

   public int getInboxSize(int objectId) {
      int size = 0;

      for (MailMessage msg : this._messages.values()) {
         if (msg != null && msg.getReceiverId() == objectId && !msg.isDeletedByReceiver()) {
            size++;
         }
      }

      return size;
   }

   public int getOutboxSize(int objectId) {
      int size = 0;

      for (MailMessage msg : this._messages.values()) {
         if (msg != null && msg.getSenderId() == objectId && !msg.isDeletedBySender()) {
            size++;
         }
      }

      return size;
   }

   public List<MailMessage> getInbox(int objectId) {
      List<MailMessage> inbox = new ArrayList<>();

      for (MailMessage msg : this._messages.values()) {
         if (msg != null && msg.getReceiverId() == objectId && !msg.isDeletedByReceiver()) {
            inbox.add(msg);
         }
      }

      return inbox;
   }

   public List<MailMessage> getOutbox(int objectId) {
      List<MailMessage> outbox = new ArrayList<>();

      for (MailMessage msg : this._messages.values()) {
         if (msg != null && msg.getSenderId() == objectId && !msg.isDeletedBySender()) {
            outbox.add(msg);
         }
      }

      return outbox;
   }

   public void sendMessage(MailMessage msg) {
      this._messages.put(msg.getId(), msg);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = MailMessage.getStatement(msg, con);
      ) {
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error saving message:", (Throwable)var10);
      }

      Player receiver = World.getPlayer(msg.getReceiverId());
      if (receiver != null) {
         receiver.sendPacket(ExNoticePostArrived.valueOf(true));
      }

      MessageDeletionTaskManager.getInstance().add(msg.getId(), msg.getExpiration());
   }

   public void markAsReadInDb(int msgId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE messages SET isUnread = 'false' WHERE messageId = ?");
      ) {
         ps.setInt(1, msgId);
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error marking as read message:", (Throwable)var10);
      }
   }

   public void markAsDeletedBySenderInDb(int msgId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedBySender = 'true' WHERE messageId = ?");
      ) {
         ps.setInt(1, msgId);
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error marking as deleted by sender message:", (Throwable)var10);
      }
   }

   public void markAsDeletedByReceiverInDb(int msgId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedByReceiver = 'true' WHERE messageId = ?");
      ) {
         ps.setInt(1, msgId);
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error marking as deleted by receiver message:", (Throwable)var10);
      }
   }

   public void removeAttachmentsInDb(int msgId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE messages SET hasAttachments = 'false' WHERE messageId = ?");
      ) {
         ps.setInt(1, msgId);
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error removing attachments in message:", (Throwable)var10);
      }
   }

   public void deleteMessageInDb(int msgId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM messages WHERE messageId = ?");
      ) {
         ps.setInt(1, msgId);
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error deleting message:", (Throwable)var10);
      }

      this._messages.remove(msgId);
      IdManager.getInstance().releaseId(msgId);
   }

   public static MailManager getInstance() {
      return MailManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final MailManager INSTANCE = new MailManager();
   }
}
