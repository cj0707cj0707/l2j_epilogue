package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.Manager.PostBBSManager;

public class Post {
   private static final Logger LOGGER = Logger.getLogger(Post.class.getName());
   private final Collection<Post.CPost> _post = ConcurrentHashMap.newKeySet();

   public Post(String postOwner, int postOwnerId, long date, int tid, int postForumId, String txt) {
      Post.CPost cp = new Post.CPost();
      cp.setPostId(0);
      cp.setPostOwner(postOwner);
      cp.setPostOwnerId(postOwnerId);
      cp.setPostDate(date);
      cp.setPostTopicId(tid);
      cp.setPostForumId(postForumId);
      cp.setPostText(txt);
      this._post.add(cp);
      this.insertindb(cp);
   }

   private void insertindb(Post.CPost cp) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO posts (post_id,post_owner_name,post_ownerid,post_date,post_topic_id,post_forum_id,post_txt) values (?,?,?,?,?,?,?)"
         );
      ) {
         ps.setInt(1, cp.getPostId());
         ps.setString(2, cp.getPostOwner());
         ps.setInt(3, cp.getPostOwnerId());
         ps.setLong(4, cp.getPostDate());
         ps.setInt(5, cp.getPostTopicId());
         ps.setInt(6, cp.getPostForumId());
         ps.setString(7, cp.getPostText());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Error while saving new Post to db " + var10.getMessage(), (Throwable)var10);
      }
   }

   public Post(Topic t) {
      this.load(t);
   }

   public Post.CPost getCPost(int id) {
      int i = 0;

      for (Post.CPost cp : this._post) {
         if (i++ == id) {
            return cp;
         }
      }

      return null;
   }

   public void deleteMe(Topic t) {
      PostBBSManager.getInstance().delPostByTopic(t);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM posts WHERE post_forum_id=? AND post_topic_id=?");
      ) {
         ps.setInt(1, t.getForumID());
         ps.setInt(2, t.getID());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Error while deleting post: " + var10.getMessage(), (Throwable)var10);
      }
   }

   private void load(Topic t) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM posts WHERE post_forum_id=? AND post_topic_id=? ORDER BY post_id ASC");
      ) {
         ps.setInt(1, t.getForumID());
         ps.setInt(2, t.getID());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Post.CPost cp = new Post.CPost();
               cp.setPostId(rs.getInt("post_id"));
               cp.setPostOwner(rs.getString("post_owner_name"));
               cp.setPostOwnerId(rs.getInt("post_ownerid"));
               cp.setPostDate(rs.getLong("post_date"));
               cp.setPostTopicId(rs.getInt("post_topic_id"));
               cp.setPostForumId(rs.getInt("post_forum_id"));
               cp.setPostText(rs.getString("post_txt"));
               this._post.add(cp);
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Data error on Post " + t.getForumID() + "/" + t.getID() + " : " + var13.getMessage(), (Throwable)var13);
      }
   }

   public void updateText(int i) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE posts SET post_txt=? WHERE post_id=? AND post_topic_id=? AND post_forum_id=?");
      ) {
         Post.CPost cp = this.getCPost(i);
         ps.setString(1, cp.getPostText());
         ps.setInt(2, cp.getPostId());
         ps.setInt(3, cp.getPostTopicId());
         ps.setInt(4, cp.getPostForumId());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Error while saving new Post to db " + var10.getMessage(), (Throwable)var10);
      }
   }

   public static class CPost {
      private int _postId;
      private String _postOwner;
      private int _postOwnerId;
      private long _postDate;
      private int _postTopicId;
      private int _postForumId;
      private String _postText;

      public void setPostId(int postId) {
         this._postId = postId;
      }

      public int getPostId() {
         return this._postId;
      }

      public void setPostOwner(String postOwner) {
         this._postOwner = postOwner;
      }

      public String getPostOwner() {
         return this._postOwner;
      }

      public void setPostOwnerId(int postOwnerId) {
         this._postOwnerId = postOwnerId;
      }

      public int getPostOwnerId() {
         return this._postOwnerId;
      }

      public void setPostDate(long postDate) {
         this._postDate = postDate;
      }

      public long getPostDate() {
         return this._postDate;
      }

      public void setPostTopicId(int postTopicId) {
         this._postTopicId = postTopicId;
      }

      public int getPostTopicId() {
         return this._postTopicId;
      }

      public void setPostForumId(int postForumId) {
         this._postForumId = postForumId;
      }

      public int getPostForumId() {
         return this._postForumId;
      }

      public void setPostText(String postText) {
         this._postText = postText;
      }

      public String getPostText() {
         if (this._postText == null) {
            return "";
         } else {
            String text = this._postText.toLowerCase();
            return text.contains("action") && text.contains("bypass") ? "" : this._postText.replaceAll("<.*?>", "");
         }
      }
   }
}
