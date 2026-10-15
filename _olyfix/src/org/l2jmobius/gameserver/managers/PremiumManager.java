package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

public class PremiumManager {
   private static final Logger LOGGER = Logger.getLogger(PremiumManager.class.getName());
   private static final String LOAD_SQL = "SELECT account_name,enddate FROM account_premium WHERE account_name = ?";
   private static final String UPDATE_SQL = "REPLACE INTO account_premium (account_name,enddate) VALUE (?,?)";
   private static final String DELETE_SQL = "DELETE FROM account_premium WHERE account_name = ?";
   private final Map<String, Long> _premiumData = new ConcurrentHashMap<>();
   private final Map<String, ScheduledFuture<?>> _expiretasks = new ConcurrentHashMap<>();
   private final ListenersContainer _listenerContainer = Containers.Players();
   private final Consumer<OnPlayerLogin> _playerLoginEvent = event -> {
      Player player = event.getPlayer();
      String accountName = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName() : player.getName();
      this.loadPremiumData(accountName);
      long now = System.currentTimeMillis();
      long premiumExpiration = this.getPremiumExpiration(accountName);
      player.setPremiumStatus(premiumExpiration > now);
      if (player.hasPremiumStatus()) {
         this.startExpireTask(player, premiumExpiration - now);
      } else if (premiumExpiration > 0L) {
         this.removePremiumStatus(accountName, false);
      }
   };
   private final Consumer<OnPlayerLogout> _playerLogoutEvent = event -> this.stopExpireTask(event.getPlayer());

   protected PremiumManager() {
      this._listenerContainer.addListener(new ConsumerEventListener(this._listenerContainer, EventType.ON_PLAYER_LOGIN, this._playerLoginEvent, this));
      this._listenerContainer.addListener(new ConsumerEventListener(this._listenerContainer, EventType.ON_PLAYER_LOGOUT, this._playerLogoutEvent, this));
   }

   private void startExpireTask(Player player, long delay) {
      this._expiretasks
         .put(
            PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName().toLowerCase() : player.getName().toLowerCase(),
            ThreadPool.schedule(new PremiumManager.PremiumExpireTask(player), delay)
         );
   }

   private void stopExpireTask(Player player) {
      ScheduledFuture<?> task = this._expiretasks
         .remove(PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? player.getAccountName().toLowerCase() : player.getName().toLowerCase());
      if (task != null) {
         task.cancel(false);
      }
   }

   private void loadPremiumData(String accountName) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement stmt = con.prepareStatement("SELECT account_name,enddate FROM account_premium WHERE account_name = ?");
      ) {
         stmt.setString(1, accountName.toLowerCase());

         try (ResultSet rset = stmt.executeQuery()) {
            while (rset.next()) {
               this._premiumData.put(rset.getString(1).toLowerCase(), rset.getLong(2));
            }
         }
      } catch (SQLException var13) {
         LOGGER.warning("Problem with PremiumManager: " + var13.getMessage());
      }
   }

   public long getPremiumExpiration(String accountName) {
      return this._premiumData.getOrDefault(accountName.toLowerCase(), 0L);
   }

   public void addPremiumTime(String accountName, int timeValue, TimeUnit timeUnit) {
      long addTime = timeUnit.toMillis(timeValue);
      long now = System.currentTimeMillis();
      long oldPremiumExpiration = Math.max(now, this.getPremiumExpiration(accountName));
      long newPremiumExpiration = oldPremiumExpiration + addTime;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement stmt = con.prepareStatement("REPLACE INTO account_premium (account_name,enddate) VALUE (?,?)");
      ) {
         stmt.setString(1, accountName.toLowerCase());
         stmt.setLong(2, newPremiumExpiration);
         stmt.execute();
      } catch (SQLException var20) {
         LOGGER.warning("Problem with PremiumManager: " + var20.getMessage());
      }

      this._premiumData.put(accountName.toLowerCase(), newPremiumExpiration);

      for (Player player : World.getPlayers()) {
         if (accountName.equalsIgnoreCase(player.getAccountName())) {
            this.stopExpireTask(player);
            this.startExpireTask(player, newPremiumExpiration - now);
            if (!player.hasPremiumStatus()) {
               player.setPremiumStatus(true);
            }
            break;
         }
      }
   }

   public void removePremiumStatus(String accountName, boolean checkOnline) {
      if (checkOnline) {
         for (Player player : World.getPlayers()) {
            if (accountName.equalsIgnoreCase(player.getAccountName()) && player.hasPremiumStatus()) {
               player.setPremiumStatus(false);
               this.stopExpireTask(player);
               break;
            }
         }
      }

      this._premiumData.remove(accountName.toLowerCase());

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement stmt = con.prepareStatement("DELETE FROM account_premium WHERE account_name = ?");
      ) {
         stmt.setString(1, accountName.toLowerCase());
         stmt.execute();
      } catch (SQLException var11) {
         LOGGER.warning("Problem with PremiumManager: " + var11.getMessage());
      }
   }

   public static PremiumManager getInstance() {
      return PremiumManager.SingletonHolder.INSTANCE;
   }

   class PremiumExpireTask implements Runnable {
      final Player _player;

      PremiumExpireTask(Player player) {
         Objects.requireNonNull(PremiumManager.this);
         super();
         this._player = player;
      }

      @Override
      public void run() {
         this._player.setPremiumStatus(false);
      }
   }

   private static class SingletonHolder {
      protected static final PremiumManager INSTANCE = new PremiumManager();
   }
}
