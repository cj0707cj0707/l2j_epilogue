package org.l2jmobius.gameserver.managers;

import java.lang.management.ManagementFactory;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

public class PrecautionaryRestartManager {
   private static final Logger LOGGER = Logger.getLogger(PrecautionaryRestartManager.class.getName());
   private static final String SYSTEM_CPU_LOAD_VAR = "SystemCpuLoad";
   private static final String PROCESS_CPU_LOAD_VAR = "ProcessCpuLoad";
   private static boolean _restarting = false;

   protected PrecautionaryRestartManager() {
      ThreadPool.scheduleAtFixedRate(() -> {
         if (!_restarting) {
            if (ServerConfig.PRECAUTIONARY_RESTART_CPU && getCpuLoad("SystemCpuLoad") > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE) {
               if (this.serverBizzy()) {
                  return;
               }

               LOGGER.info("PrecautionaryRestartManager: CPU usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
               LOGGER.info("PrecautionaryRestartManager: Server is using " + getCpuLoad("ProcessCpuLoad") + "%.");
               World.broadcastToAllOnlinePlayers("Server will restart in 10 minutes.", false);
               Shutdown.getInstance().startShutdown(null, 600, true);
            }

            if (ServerConfig.PRECAUTIONARY_RESTART_MEMORY && getProcessRamLoad() > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE) {
               if (this.serverBizzy()) {
                  return;
               }

               LOGGER.info("PrecautionaryRestartManager: Memory usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
               World.broadcastToAllOnlinePlayers("Server will restart in 10 minutes.", false);
               Shutdown.getInstance().startShutdown(null, 600, true);
            }
         }
      }, ServerConfig.PRECAUTIONARY_RESTART_DELAY, ServerConfig.PRECAUTIONARY_RESTART_DELAY);
   }

   private static double getCpuLoad(String var) {
      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
         AttributeList list = mbs.getAttributes(name, new String[]{var});
         if (list.isEmpty()) {
            return 0.0;
         } else {
            Attribute att = (Attribute)list.get(0);
            Double value = (Double)att.getValue();
            return value == -1.0 ? 0.0 : value * 1000.0 / 10.0;
         }
      } catch (Exception var6) {
         return 0.0;
      }
   }

   private static double getProcessRamLoad() {
      Runtime runTime = Runtime.getRuntime();
      long totalMemory = runTime.maxMemory();
      long usedMemory = totalMemory - (totalMemory - runTime.totalMemory() + runTime.freeMemory());
      return usedMemory * 100L / totalMemory;
   }

   private boolean serverBizzy() {
      for (Castle castle : CastleManager.getInstance().getCastles()) {
         if (castle != null && castle.getSiege().isInProgress()) {
            return true;
         }
      }

      for (Fort fort : FortManager.getInstance().getForts()) {
         if (fort != null && fort.getSiege().isInProgress()) {
            return true;
         }
      }

      for (Player player : World.getPlayers()) {
         if (player != null && !player.isInOfflineMode()) {
            if (player.isInOlympiadMode()) {
               return true;
            }

            if (player.isOnEvent()) {
               return true;
            }

            if (player.isInInstance()) {
               return true;
            }

            WorldObject target = player.getTarget();
            if (target instanceof RaidBoss || target instanceof GrandBoss) {
               return true;
            }
         }
      }

      return false;
   }

   public void restartEnabled() {
      _restarting = true;
   }

   public void restartAborted() {
      _restarting = false;
   }

   public static PrecautionaryRestartManager getInstance() {
      return PrecautionaryRestartManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PrecautionaryRestartManager INSTANCE = new PrecautionaryRestartManager();
   }
}
