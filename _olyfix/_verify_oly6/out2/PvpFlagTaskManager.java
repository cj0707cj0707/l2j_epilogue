package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PvpFlagTaskManager implements Runnable {
   private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
   private static boolean _working = false;

   protected PvpFlagTaskManager() {
      ThreadPool.schedulePriorityTaskAtFixedRate(this, 1000L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PLAYERS.isEmpty()) {
            long currentTime = System.currentTimeMillis();

            for (Player player : PLAYERS) {
               if (currentTime > player.getPvpFlagLasts()) {
                  player.stopPvPFlag();
               } else if (currentTime > player.getPvpFlagLasts() - 20000L) {
                  player.updatePvPFlag(2);
               } else {
                  player.updatePvPFlag(1);
               }
            }
         }

         _working = false;
      }
   }

   public void add(Player player) {
      PLAYERS.add(player);
   }

   public void remove(Player player) {
      PLAYERS.remove(player);
   }

   public static org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager();
   }
}
