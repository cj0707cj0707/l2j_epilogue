package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.data.holders.PunishmentHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.tasks.player.IllegalPlayerActionTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

public class PunishmentManager {
   private static final Logger LOGGER = Logger.getLogger(PunishmentManager.class.getName());
   private final Map<PunishmentAffect, PunishmentHolder> _tasks = new ConcurrentHashMap<>();

   protected PunishmentManager() {
      this.load();
   }

   private void load() {
      for (PunishmentAffect affect : PunishmentAffect.values()) {
         this._tasks.put(affect, new PunishmentHolder());
      }

      int initiated = 0;
      int expired = 0;

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement st = con.createStatement();
         ResultSet rset = st.executeQuery("SELECT * FROM punishments");
      ) {
         while (rset.next()) {
            int id = rset.getInt("id");
            String key = rset.getString("key");
            PunishmentAffect affect = PunishmentAffect.getByName(rset.getString("affect"));
            PunishmentType type = PunishmentType.getByName(rset.getString("type"));
            long expirationTime = rset.getLong("expiration");
            if (type != null && affect != null) {
               if (expirationTime > 0L && System.currentTimeMillis() > expirationTime) {
                  expired++;
               } else {
                  initiated++;
                  this._tasks
                     .get(affect)
                     .addPunishment(new PunishmentTask(id, key, affect, type, expirationTime, rset.getString("reason"), rset.getString("punishedBy"), true));
               }
            }
         }
      } catch (Exception var18) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Error while loading punishments: " + var18.getMessage());
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + initiated + " active and " + expired + " expired punishments.");
   }

   public void startPunishment(PunishmentTask task) {
      PunishmentAffect affect = task.getAffect();
      if (task.getType() == PunishmentType.BAN) {
         switch (affect) {
            case ACCOUNT:
               String accountName = (String)task.getKey();
               LoginServerThread.getInstance().sendAccessLevel(accountName, -1);
               break;
            case CHARACTER:
               String charId = String.valueOf(task.getKey());
               this.changeCharAccessLevel(charId, -1);
         }
      }

      this._tasks.get(affect).addPunishment(task);
   }

   public void stopPunishment(Object key, PunishmentAffect affect, PunishmentType type) {
      PunishmentTask task = this.getPunishment(key, affect, type);
      if (task != null) {
         if (type == PunishmentType.BAN) {
            switch (affect) {
               case ACCOUNT:
                  String accountName = (String)task.getKey();
                  LoginServerThread.getInstance().sendAccessLevel(accountName, 0);
                  break;
               case CHARACTER:
                  String charId = String.valueOf(task.getKey());
                  this.changeCharAccessLevel(charId, 0);
            }
         }

         this._tasks.get(affect).stopPunishment(task);
      }
   }

   private void changeCharAccessLevel(String charId, int lvl) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE characters SET accessLevel=? WHERE charId=?");
      ) {
         ps.setInt(1, lvl);
         ps.setString(2, charId);
         ps.executeUpdate();
      } catch (SQLException var11) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Error updating access level for character: " + charId + ". " + var11.getMessage());
      }
   }

   public boolean hasPunishment(Object key, PunishmentAffect affect, PunishmentType type) {
      PunishmentHolder holder = this._tasks.get(affect);
      return holder.hasPunishment(String.valueOf(key), type);
   }

   public long getPunishmentExpiration(Object key, PunishmentAffect affect, PunishmentType type) {
      PunishmentTask p = this.getPunishment(key, affect, type);
      return p != null ? p.getExpirationTime() : 0L;
   }

   public PunishmentTask getPunishment(Object key, PunishmentAffect affect, PunishmentType type) {
      return this._tasks.get(affect).getPunishment(String.valueOf(key), type);
   }

   public static void handleIllegalPlayerAction(Player actor, String message, IllegalActionPunishmentType punishment) {
      ThreadPool.schedule(new IllegalPlayerActionTask(actor, message, punishment), 5000L);
   }

   public static PunishmentManager getInstance() {
      return PunishmentManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PunishmentManager INSTANCE = new PunishmentManager();
   }
}
