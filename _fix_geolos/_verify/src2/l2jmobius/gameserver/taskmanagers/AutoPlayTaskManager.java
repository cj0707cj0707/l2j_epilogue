package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.AutoPlay;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.SingletonHolder;

public class AutoPlayTaskManager {
   private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
   private static final Map<Player, Integer> IDLE_COUNT = new ConcurrentHashMap<>();
   private static final int POOL_SIZE = 200;
   private static final int TASK_DELAY = 700;
   private static final Integer AUTO_ATTACK_ACTION = 2;

   protected AutoPlayTaskManager() {
   }

   public synchronized void startAutoPlay(Player player) {
      for (Set<Player> pool : POOLS) {
         if (pool.contains(player)) {
            return;
         }
      }

      player.setAutoPlaying(true);

      for (Set<Player> poolx : POOLS) {
         if (poolx.size() < 200) {
            player.onActionRequest();
            poolx.add(player);
            return;
         }
      }

      Set<Player> poolxx = ConcurrentHashMap.newKeySet(200);
      player.onActionRequest();
      poolxx.add(player);
      ThreadPool.schedulePriorityTaskAtFixedRate(new AutoPlay(this, poolxx), 700L, 700L);
      POOLS.add(poolxx);
   }

   public void stopAutoPlay(Player player) {
      for (Set<Player> pool : POOLS) {
         if (pool.remove(player)) {
            player.setAutoPlaying(false);
            if (player.hasServitor() || player.hasPet()) {
               player.getSummon().followOwner();
            }

            IDLE_COUNT.remove(player);
            return;
         }
      }
   }

   public static AutoPlayTaskManager getInstance() {
      return SingletonHolder.INSTANCE;
   }
}
