package org.l2jmobius.gameserver.mechanics.announce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;

public class AutoAnnouncement extends Announcement implements Runnable {
   private static final String INSERT_QUERY = "INSERT INTO announcements (`type`, `content`, `author`, `initial`, `delay`, `repeat`) VALUES (?, ?, ?, ?, ?, ?)";
   private static final String UPDATE_QUERY = "UPDATE announcements SET `type` = ?, `content` = ?, `author` = ?, `initial` = ?, `delay` = ?, `repeat` = ? WHERE id = ?";
   private long _initial;
   private long _delay;
   private int _repeat = -1;
   private int _currentState;
   private ScheduledFuture<?> _task;

   public AutoAnnouncement(AnnouncementType type, String content, String author, long initial, long delay, int repeat) {
      super(type, content, author);
      this._initial = initial;
      this._delay = delay;
      this._repeat = repeat;
      this.restartMe();
   }

   public AutoAnnouncement(ResultSet rset) throws SQLException {
      super(rset);
      this._initial = rset.getLong("initial");
      this._delay = rset.getLong("delay");
      this._repeat = rset.getInt("repeat");
      this.restartMe();
   }

   public long getInitial() {
      return this._initial;
   }

   public void setInitial(long initial) {
      this._initial = initial;
   }

   public long getDelay() {
      return this._delay;
   }

   public void setDelay(long delay) {
      this._delay = delay;
   }

   public int getRepeat() {
      return this._repeat;
   }

   public void setRepeat(int repeat) {
      this._repeat = repeat;
   }

   @Override
   public boolean storeMe() {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "INSERT INTO announcements (`type`, `content`, `author`, `initial`, `delay`, `repeat`) VALUES (?, ?, ?, ?, ?, ?)", 1
            );
         ) {
            ps.setInt(1, this.getType().ordinal());
            ps.setString(2, this.getContent());
            ps.setString(3, this.getAuthor());
            ps.setLong(4, this._initial);
            ps.setLong(5, this._delay);
            ps.setInt(6, this._repeat);
            ps.execute();

            try (ResultSet rset = ps.getGeneratedKeys()) {
               if (rset.next()) {
                  this._id = rset.getInt(1);
               }
            }
         }

         return true;
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't store announcement: ", (Throwable)var12);
         return false;
      }
   }

   @Override
   public boolean updateMe() {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "UPDATE announcements SET `type` = ?, `content` = ?, `author` = ?, `initial` = ?, `delay` = ?, `repeat` = ? WHERE id = ?"
            );
         ) {
            ps.setInt(1, this.getType().ordinal());
            ps.setString(2, this.getContent());
            ps.setString(3, this.getAuthor());
            ps.setLong(4, this._initial);
            ps.setLong(5, this._delay);
            ps.setLong(6, this._repeat);
            ps.setLong(7, this.getId());
            ps.execute();
         }

         return true;
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't update announcement: ", (Throwable)var9);
         return false;
      }
   }

   @Override
   public boolean deleteMe() {
      if (this._task != null && !this._task.isCancelled()) {
         this._task.cancel(false);
      }

      return super.deleteMe();
   }

   public void restartMe() {
      if (this._task != null && !this._task.isCancelled()) {
         this._task.cancel(false);
      }

      this._currentState = this._repeat;
      this._task = ThreadPool.schedule(this, this._initial);
   }

   @Override
   public void run() {
      if (this._currentState == -1 || this._currentState > 0) {
         for (String content : this.getContent().split(System.lineSeparator())) {
            World.broadcastToAllOnlinePlayers(content, this.getType() == AnnouncementType.AUTO_CRITICAL);
         }

         if (this._currentState != -1) {
            this._currentState--;
         }

         this._task = ThreadPool.schedule(this, this._delay);
      }
   }
}
