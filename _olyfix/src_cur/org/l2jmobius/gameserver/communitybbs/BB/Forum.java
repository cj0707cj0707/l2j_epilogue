package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;

public class Forum {
   private static final Logger LOGGER = Logger.getLogger(Forum.class.getName());
   public static final int ROOT = 0;
   public static final int NORMAL = 1;
   public static final int CLAN = 2;
   public static final int MEMO = 3;
   public static final int MAIL = 4;
   public static final int INVISIBLE = 0;
   public static final int ALL = 1;
   public static final int CLANMEMBERONLY = 2;
   public static final int OWNERONLY = 3;
   private final Collection<Forum> _children;
   private final Map<Integer, Topic> _topic = new ConcurrentHashMap<>();
   private final int _forumId;
   private String _forumName;
   private int _forumType;
   private int _forumPost;
   private int _forumPerm;
   private final Forum _fParent;
   private int _ownerID;
   private boolean _loaded = false;

   public Forum(int forumId, Forum fParent) {
      this._forumId = forumId;
      this._fParent = fParent;
      this._children = ConcurrentHashMap.newKeySet();
   }

   public Forum(String name, Forum parent, int type, int perm, int ownerId) {
      this._forumName = name;
      this._forumId = ForumsBBSManager.getInstance().getANewID();
      this._forumType = type;
      this._forumPost = 0;
      this._forumPerm = perm;
      this._fParent = parent;
      this._ownerID = ownerId;
      this._children = ConcurrentHashMap.newKeySet();
      parent._children.add(this);
      ForumsBBSManager.getInstance().addForum(this);
      this._loaded = true;
   }

   private void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM forums WHERE forum_id=?");
      ) {
         ps.setInt(1, this._forumId);

         try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
               this._forumName = rs.getString("forum_name");
               this._forumPost = rs.getInt("forum_post");
               this._forumType = rs.getInt("forum_type");
               this._forumPerm = rs.getInt("forum_perm");
               this._ownerID = rs.getInt("forum_owner_id");
            }
         }
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, "Data error on Forum " + this._forumId + " : " + var19.getMessage(), (Throwable)var19);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM topic WHERE topic_forum_id=? ORDER BY topic_id DESC");
      ) {
         ps.setInt(1, this._forumId);

         try (ResultSet rsx = ps.executeQuery()) {
            while (rsx.next()) {
               Topic t = new Topic(
                  TopicConstructorType.RESTORE,
                  rsx.getInt("topic_id"),
                  rsx.getInt("topic_forum_id"),
                  rsx.getString("topic_name"),
                  rsx.getLong("topic_date"),
                  rsx.getString("topic_ownername"),
                  rsx.getInt("topic_ownerid"),
                  rsx.getInt("topic_type"),
                  rsx.getInt("topic_reply")
               );
               this._topic.put(t.getID(), t);
               if (t.getID() > TopicBBSManager.getInstance().getMaxID(this)) {
                  TopicBBSManager.getInstance().setMaxID(t.getID(), this);
               }
            }
         }
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Data error on Forum " + this._forumId + " : " + var15.getMessage(), (Throwable)var15);
      }
   }

   private void getChildren() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_parent=?");
      ) {
         ps.setInt(1, this._forumId);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Forum f = new Forum(rs.getInt("forum_id"), this);
               this._children.add(f);
               ForumsBBSManager.getInstance().addForum(f);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Data error on Forum (children): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public int getTopicSize() {
      this.vload();
      return this._topic.size();
   }

   public Topic getTopic(int j) {
      this.vload();
      return this._topic.get(j);
   }

   public void addTopic(Topic t) {
      this.vload();
      this._topic.put(t.getID(), t);
   }

   public int getID() {
      return this._forumId;
   }

   public String getName() {
      this.vload();
      return this._forumName;
   }

   public int getType() {
      this.vload();
      return this._forumType;
   }

   public Forum getChildByName(String name) {
      this.vload();

      for (Forum forum : this._children) {
         if (forum.getName().equals(name)) {
            return forum;
         }
      }

      return null;
   }

   public void rmTopicByID(int id) {
      this._topic.remove(id);
   }

   public void insertIntoDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO forums (forum_id,forum_name,forum_parent,forum_post,forum_type,forum_perm,forum_owner_id) VALUES (?,?,?,?,?,?,?)"
         );
      ) {
         ps.setInt(1, this._forumId);
         ps.setString(2, this._forumName);
         ps.setInt(3, this._fParent.getID());
         ps.setInt(4, this._forumPost);
         ps.setInt(5, this._forumType);
         ps.setInt(6, this._forumPerm);
         ps.setInt(7, this._ownerID);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Error while saving new Forum to db " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void vload() {
      if (!this._loaded) {
         this.load();
         this.getChildren();
         this._loaded = true;
      }
   }
}
