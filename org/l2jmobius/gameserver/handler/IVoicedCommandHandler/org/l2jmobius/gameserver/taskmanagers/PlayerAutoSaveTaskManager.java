package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PlayerAutoSaveTaskManager implements Runnable {
   private static final Map<Player, Long> PLAYER_TIMES = new ConcurrentHashMap<>();
   private static boolean _working = false;

   protected PlayerAutoSaveTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 1000L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PLAYER_TIMES.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<Player, Long>> iterator = PLAYER_TIMES.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Player, Long> entry = iterator.next();
               Player player = entry.getKey();
               Long time = entry.getValue();
               if (currentTime > time) {
                  if (player != null && player.isOnline()) {
                     player.autoSave();
                     PLAYER_TIMES.put(player, currentTime + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
                     break;
                  }

                  iterator.remove();
               }
            }
         }

         _working = false;
      }
   }

   public void add(Player player) {
      PLAYER_TIMES.put(player, System.currentTimeMillis() + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
   }

   public void remove(Player player) {
      PLAYER_TIMES.remove(player);
   }

   public static PlayerAutoSaveTaskManager getInstance() {
      return PlayerAutoSaveTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PlayerAutoSaveTaskManager INSTANCE = new PlayerAutoSaveTaskManager();
   }
}
