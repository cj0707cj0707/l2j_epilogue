package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;

public class Topic {
   private static final Logger LOGGER = Logger.getLogger(Topic.class.getName());
   public static final int NORMAL = 0;
   public static final int MEMO = 1;
   private final int _id;
   private final int _forumId;
   private final String _topicName;
   private final long _date;
   private final String _ownerName;
   private final int _ownerId;
   private final int _type;
   private final int _cReply;

   public Topic(TopicConstructorType ct, int id, int fid, String name, long date, String oname, int oid, int type, int cReply) {
      this._id = id;
      this._forumId = fid;
      this._topicName = name;
      this._date = date;
      this._ownerName = oname;
      this._ownerId = oid;
      this._type = type;
      this._cReply = cReply;
      TopicBBSManager.getInstance().addTopic(this);
      if (ct == TopicConstructorType.CREATE) {
         this.insertindb();
      }
   }

   private void insertindb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO topic (topic_id,topic_forum_id,topic_name,topic_date,topic_ownername,topic_ownerid,topic_type,topic_reply) values (?,?,?,?,?,?,?,?)"
         );
      ) {
         ps.setInt(1, this._id);
         ps.setInt(2, this._forumId);
         ps.setString(3, this._topicName);
         ps.setLong(4, this._date);
         ps.setString(5, this._ownerName);
         ps.setInt(6, this._ownerId);
         ps.setInt(7, this._type);
         ps.setInt(8, this._cReply);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Error while saving new Topic to db " + var9.getMessage(), (Throwable)var9);
      }
   }

   public int getID() {
      return this._id;
   }

   public int getForumID() {
      return this._forumId;
   }

   public String getName() {
      return this._topicName;
   }

   public String getOwnerName() {
      return this._ownerName;
   }

   public void deleteme(Forum f) {
      TopicBBSManager.getInstance().delTopic(this);
      f.rmTopicByID(this._id);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM topic WHERE topic_id=? AND topic_forum_id=?");
      ) {
         ps.setInt(1, this._id);
         ps.setInt(2, f.getID());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Error while deleting topic: " + var10.getMessage(), (Throwable)var10);
      }
   }

   public long getDate() {
      return this._date;
   }
}
