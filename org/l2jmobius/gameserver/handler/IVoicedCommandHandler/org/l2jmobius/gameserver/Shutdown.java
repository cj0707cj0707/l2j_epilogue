package org.l2jmobius.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.DatabaseConfig;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FishingChampionshipManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.PrecautionaryRestartManager;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class Shutdown extends Thread {
   private static final Logger LOGGER = Logger.getLogger(Shutdown.class.getName());
   private static final int SIGTERM = 0;
   private static final int GM_SHUTDOWN = 1;
   private static final int GM_RESTART = 2;
   private static final int ABORT = 3;
   private static final String[] MODE_TEXT = new String[]{"SIGTERM", "shutting down", "restarting", "aborting"};
   private static Shutdown _counterInstance;
   private static boolean _countdownFinished;
   private static volatile int _secondsShut;
   private static volatile int _shutdownMode;

   private void sendServerQuit(int seconds) {
      SystemMessage sysm = new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECOND_S_PLEASE_FIND_A_SAFE_PLACE_TO_LOG_OUT);
      sysm.addInt(seconds);
      World.broadcastToAllOnlinePlayers(sysm);
   }

   protected Shutdown() {
      _secondsShut = -1;
      _shutdownMode = 0;
   }

   public Shutdown(int seconds, boolean restart) {
      _secondsShut = Math.max(0, seconds);
      _shutdownMode = restart ? 2 : 1;
   }

   @Override
   public void run() {
      if (this == getInstance()) {
         this.startShutdownActions();
      } else if (!_countdownFinished) {
         this.countdown();
         LOGGER.warning("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
         switch (_shutdownMode) {
            case 1:
               getInstance().setMode(1);
               this.startShutdownActions();
               System.exit(0);
               break;
            case 2:
               getInstance().setMode(2);
               this.startShutdownActions();
               System.exit(2);
               break;
            case 3:
               LoginServerThread.getInstance().setServerStatus(0);
         }
      }
   }

   public void startShutdown(Player player, int seconds, boolean restart) {
      _shutdownMode = restart ? 2 : 1;
      if (player != null) {
         LOGGER.warning(
            "GM: " + player.getName() + "(" + player.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!"
         );
      } else {
         LOGGER.warning("Server scheduled restart issued shutdown command. " + (restart ? "Restart" : "Shutdown") + " in " + seconds + " seconds!");
      }

      if (_shutdownMode > 0) {
         switch (seconds) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 10:
            case 30:
            case 60:
            case 120:
            case 180:
            case 240:
            case 300:
            case 360:
            case 420:
            case 480:
            case 540:
               break;
            default:
               this.sendServerQuit(seconds);
         }
      }

      if (_counterInstance != null) {
         _counterInstance.abort();
      }

      if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED) {
         PrecautionaryRestartManager.getInstance().restartEnabled();
      }

      _counterInstance = new Shutdown(seconds, restart);
      _counterInstance.start();
   }

   public void abort(Player player) {
      if (_countdownFinished) {
         LOGGER.warning(
            "GM: " + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "") + "shutdown ABORT failed because countdown has finished."
         );
      } else {
         LOGGER.warning(
            "GM: "
               + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "")
               + "issued shutdown ABORT. "
               + MODE_TEXT[_shutdownMode]
               + " has been stopped!"
         );
         if (_counterInstance != null) {
            _counterInstance.abort();
            if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED) {
               PrecautionaryRestartManager.getInstance().restartAborted();
            }

            World.broadcastToAllOnlinePlayers("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!", false);
         }
      }
   }

   private void setMode(int mode) {
      _shutdownMode = mode;
   }

   private void abort() {
      _shutdownMode = 3;
   }

   private void countdown() {
      try {
         while (_secondsShut > 0) {
            if (_shutdownMode != 3) {
               switch (_secondsShut) {
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                  case 5:
                  case 10:
                  case 30:
                  case 60:
                  case 120:
                  case 180:
                  case 240:
                  case 300:
                  case 360:
                  case 420:
                  case 480:
                  case 540:
                     this.sendServerQuit(_secondsShut);
                  default:
                     if (_secondsShut <= 60 && LoginServerThread.getInstance().getServerStatus() != 4) {
                        LoginServerThread.getInstance().setServerStatus(4);
                     }

                     _secondsShut--;
                     Thread.sleep(1000L);
               }
            } else {
               if (LoginServerThread.getInstance().getServerStatus() == 4) {
                  LoginServerThread.getInstance().setServerStatus(GeneralConfig.SERVER_GMONLY ? 5 : 0);
               }
               break;
            }
         }
      } catch (Exception var2) {
      }
   }

   private void startShutdownActions() {
      if (!_countdownFinished) {
         _countdownFinished = true;
         Shutdown.TimeCounter tc = new Shutdown.TimeCounter();
         Shutdown.TimeCounter tc1 = new Shutdown.TimeCounter();

         try {
            if ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE)
               && OfflineTradeConfig.RESTORE_OFFLINERS
               && !OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME) {
               OfflineTraderTable.getInstance().storeOffliners();
               LOGGER.info("Offline Traders Table: Offline shops stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
            }
         } catch (Throwable var10) {
            LOGGER.log(Level.WARNING, "Error saving offline shops.", var10);
         }

         try {
            if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS && AutoPlayConfig.ENABLE_AUTO_ASSIST) {
               OfflinePlayTable.getInstance().storeOfflineGroups();
               LOGGER.info("Offline Play Table: Offline play groups stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
            }
         } catch (Throwable var9) {
            LOGGER.log(Level.WARNING, "Error saving offline play groups.", var9);
         }

         try {
            this.disconnectAllCharacters();
            LOGGER.info("All players disconnected and saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         } catch (Throwable var8) {
         }

         try {
            GameTimeTaskManager.getInstance().interrupt();
            LOGGER.info("Game Time Task Manager: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         } catch (Throwable var7) {
         }

         try {
            ThreadPool.shutdown();
            LOGGER.info("Thread Pool Manager: Manager has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         } catch (Throwable var6) {
         }

         try {
            LoginServerThread.getInstance().interrupt();
            LOGGER.info("Login Server Thread: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         } catch (Throwable var5) {
         }

         this.saveData();
         tc.restartCounter();

         try {
            DatabaseFactory.close();
            LOGGER.info("Database Factory: Database connection has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         } catch (Throwable var4) {
         }

         if (DatabaseConfig.BACKUP_DATABASE) {
            DatabaseBackup.performBackup("game");
         }

         LOGGER.info("The server has been successfully shut down in " + tc1.getEstimatedTime() / 1000L + "seconds.");
      }
   }

   private void saveData() {
      switch (_shutdownMode) {
         case 0:
            LOGGER.info("SIGTERM received. Shutting down NOW!");
            break;
         case 1:
            LOGGER.info("GM shutdown received. Shutting down NOW!");
            break;
         case 2:
            LOGGER.info("GM restart received. Restarting NOW!");
      }

      Shutdown.TimeCounter tc = new Shutdown.TimeCounter();
      if (!SevenSigns.getInstance().isSealValidationPeriod()) {
         SevenSignsFestival.getInstance().saveFestivalData(false);
         LOGGER.info("SevenSignsFestival: Festival data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      }

      SevenSigns.getInstance().saveSevenSignsData();
      LOGGER.info("SevenSigns: Seven Signs data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      SevenSigns.getInstance().saveSevenSignsStatus();
      LOGGER.info("SevenSigns: Seven Signs status saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      RaidBossSpawnManager.getInstance().cleanUp();
      LOGGER.info("RaidBossSpawnManager: All raidboss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      GrandBossManager.getInstance().cleanUp();
      LOGGER.info("GrandBossManager: All Grand Boss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      ItemAuctionManager.getInstance().shutdown();
      LOGGER.info("Item Auction Manager: All tasks stopped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      Olympiad.getInstance().saveOlympiadStatus();
      LOGGER.info("Olympiad System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      Hero.getInstance().shutdown();
      LOGGER.info("Hero System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      ClanTable.getInstance().shutdown();
      LOGGER.info("Clan System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      CursedWeaponsManager.getInstance().saveData();
      LOGGER.info("Cursed Weapons Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      if (!GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
         CastleManorManager.getInstance().storeMe();
         LOGGER.info("Castle Manor Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      }

      CHSiegeManager.getInstance().onServerShutDown();
      LOGGER.info("CHSiegeManager: Siegable hall attacker lists saved!");
      ScriptManager.getInstance().save();
      LOGGER.info("Script Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      GlobalVariablesManager.getInstance().storeMe();
      LOGGER.info("Global Variables Manager: Variables saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      if (GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED) {
         FishingChampionshipManager.getInstance().shutdown();
         LOGGER.info("Fishing Championship Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      }

      SchemeBufferTable.getInstance().saveSchemes();
      LOGGER.info("SchemeBufferTable: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         ItemsOnGroundManager.getInstance().saveInDb();
         LOGGER.info("Items On Ground Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
         ItemsOnGroundManager.getInstance().cleanUp();
         LOGGER.info("Items On Ground Manager: Cleaned up(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
      }

      if (GeneralConfig.BOTREPORT_ENABLE) {
         BotReportTable.getInstance().saveReportedCharData();
         LOGGER.info("Bot Report Table: Successfully saved reports to database!");
      }

      try {
         Thread.sleep(5000L);
      } catch (Exception var3) {
      }
   }

   private void disconnectAllCharacters() {
      for (Player player : World.getPlayers()) {
         Disconnection.of(player).storeAndDeleteWith(ServerClose.STATIC_PACKET);
      }
   }

   public static Shutdown getInstance() {
      return Shutdown.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final Shutdown INSTANCE = new Shutdown();
   }

   private static class TimeCounter {
      private long _startTime;

      protected TimeCounter() {
         this.restartCounter();
      }

      public void restartCounter() {
         this._startTime = System.currentTimeMillis();
      }

      public long getEstimatedTimeAndRestartCounter() {
         long toReturn = System.currentTimeMillis() - this._startTime;
         this.restartCounter();
         return toReturn;
      }

      public long getEstimatedTime() {
         return System.currentTimeMillis() - this._startTime;
      }
   }
}
