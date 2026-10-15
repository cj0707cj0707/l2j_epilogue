package org.l2jmobius.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ForumsBBSManager extends BaseBBSManager {
   private static final Logger LOGGER = Logger.getLogger(ForumsBBSManager.class.getName());
   private final Collection<Forum> _table;
   private int _lastid = 1;

   protected ForumsBBSManager() {
      this._table = ConcurrentHashMap.newKeySet();

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT forum_id FROM forums WHERE forum_type = 0");
      ) {
         while (rs.next()) {
            this.addForum(new Forum(rs.getInt("forum_id"), null));
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Data error on Forum (root): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void initRoot() {
      this._table.forEach(Forum::vload);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._table.size() + " forums. Last forum id used: " + this._lastid);
   }

   public void addForum(Forum ff) {
      if (ff != null) {
         this._table.add(ff);
         if (ff.getID() > this._lastid) {
            this._lastid = ff.getID();
         }
      }
   }

   @Override
   public void parsecmd(String command, Player player) {
   }

   public Forum getForumByName(String name) {
      for (Forum forum : this._table) {
         if (forum.getName().equals(name)) {
            return forum;
         }
      }

      return null;
   }

   public Forum createNewForum(String name, Forum parent, int type, int perm, int oid) {
      Forum forum = new Forum(name, parent, type, perm, oid);
      forum.insertIntoDb();
      return forum;
   }

   public int getANewID() {
      return ++this._lastid;
   }

   public Forum getForumByID(int idf) {
      for (Forum f : this._table) {
         if (f.getID() == idf) {
            return f;
         }
      }

      return null;
   }

   @Override
   public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player) {
   }

   public static ForumsBBSManager getInstance() {
      return ForumsBBSManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ForumsBBSManager INSTANCE = new ForumsBBSManager();
   }
}
