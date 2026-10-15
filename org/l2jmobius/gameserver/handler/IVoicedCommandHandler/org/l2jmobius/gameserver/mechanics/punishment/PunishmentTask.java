package org.l2jmobius.gameserver.mechanics.punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.handler.IPunishmentHandler;
import org.l2jmobius.gameserver.handler.PunishmentHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;

public class PunishmentTask implements Runnable {
   protected static final Logger LOGGER = Logger.getLogger(PunishmentTask.class.getName());
   private static final String INSERT_QUERY = "INSERT INTO punishments (`key`, `affect`, `type`, `expiration`, `reason`, `punishedBy`) VALUES (?, ?, ?, ?, ?, ?)";
   private static final String UPDATE_QUERY = "UPDATE punishments SET expiration = ? WHERE id = ?";
   private int _id;
   private final String _key;
   private final PunishmentAffect _affect;
   private final PunishmentType _type;
   private final long _expirationTime;
   private final String _reason;
   private final String _punishedBy;
   private boolean _isStored;
   private ScheduledFuture<?> _task = null;

   public PunishmentTask(Object key, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy) {
      this(0, key, affect, type, expirationTime, reason, punishedBy, false);
   }

   public PunishmentTask(
      int id, Object key, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy, boolean isStored
   ) {
      this._id = id;
      this._key = String.valueOf(key);
      this._affect = affect;
      this._type = type;
      this._expirationTime = expirationTime;
      this._reason = reason;
      this._punishedBy = punishedBy;
      this._isStored = isStored;
      this.startPunishment();
   }

   public Object getKey() {
      return this._key;
   }

   public PunishmentAffect getAffect() {
      return this._affect;
   }

   public PunishmentType getType() {
      return this._type;
   }

   public long getExpirationTime() {
      return this._expirationTime;
   }

   public String getReason() {
      return this._reason;
   }

   public String getPunishedBy() {
      return this._punishedBy;
   }

   public boolean isStored() {
      return this._isStored;
   }

   public boolean isExpired() {
      return this._expirationTime > 0L && System.currentTimeMillis() > this._expirationTime;
   }

   private void startPunishment() {
      if (!this.isExpired()) {
         this.onStart();
         if (this._expirationTime > 0L) {
            this._task = ThreadPool.schedule(this, this._expirationTime - System.currentTimeMillis());
         }
      }
   }

   public void stopPunishment() {
      this.abortTask();
      this.onEnd();
   }

   private void abortTask() {
      if (this._task != null) {
         if (!this._task.isCancelled() && !this._task.isDone()) {
            this._task.cancel(false);
         }

         this._task = null;
      }
   }

   private void onStart() {
      if (!this._isStored) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "INSERT INTO punishments (`key`, `affect`, `type`, `expiration`, `reason`, `punishedBy`) VALUES (?, ?, ?, ?, ?, ?)", 1
            );
         ) {
            ps.setString(1, this._key);
            ps.setString(2, this._affect.name());
            ps.setString(3, this._type.name());
            ps.setLong(4, this._expirationTime);
            ps.setString(5, this._reason);
            ps.setString(6, this._punishedBy);
            ps.execute();

            try (ResultSet rset = ps.getGeneratedKeys()) {
               if (rset.next()) {
                  this._id = rset.getInt(1);
               }
            }

            this._isStored = true;
         } catch (SQLException var12) {
            LOGGER.log(
               Level.WARNING, this.getClass().getSimpleName() + ": Couldn't store punishment task for: " + this._affect + " " + this._key, (Throwable)var12
            );
         }
      }

      IPunishmentHandler handler = PunishmentHandler.getInstance().getHandler(this._type);
      if (handler != null) {
         handler.onStart(this);
      }
   }

   private void onEnd() {
      if (this._isStored) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE punishments SET expiration = ? WHERE id = ?");
         ) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, this._id);
            ps.execute();
         } catch (SQLException var9) {
            LOGGER.log(
               Level.WARNING,
               this.getClass().getSimpleName() + ": Couldn't update punishment task for: " + this._affect + " " + this._key + " id: " + this._id,
               (Throwable)var9
            );
         }
      }

      IPunishmentHandler handler = PunishmentHandler.getInstance().getHandler(this._type);
      if (handler != null) {
         handler.onEnd(this);
      }
   }

   @Override
   public void run() {
      PunishmentManager.getInstance().stopPunishment(this._key, this._affect, this._type);
   }
}
