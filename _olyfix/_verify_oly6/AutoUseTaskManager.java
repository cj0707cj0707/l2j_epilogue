package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager.AutoUse;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager.SingletonHolder;

public class AutoUseTaskManager {
   private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
   private static final int POOL_SIZE = 200;
   private static final int TASK_DELAY = 300;
   private static final int REUSE_MARGIN_TIME = 3;

   protected AutoUseTaskManager() {
   }

   public synchronized void startAutoUseTask(Player var1) {
      for (Set var3 : POOLS) {
         if (var3.contains(var1)) {
            return;
         }
      }

      for (Set var6 : POOLS) {
         if (var6.size() < 200) {
            var6.add(var1);
            return;
         }
      }

      KeySetView var5 = ConcurrentHashMap.newKeySet(200);
      var5.add(var1);
      ThreadPool.schedulePriorityTaskAtFixedRate(new AutoUse(this, var5), 300L, 300L);
      POOLS.add(var5);
   }

   public void stopAutoUseTask(Player var1) {
      var1.getAutoUseSettings().resetSkillOrder();

      for (Set var3 : POOLS) {
         if (var3.remove(var1)) {
            return;
         }
      }
   }

   public static AutoUseTaskManager getInstance() {
      return SingletonHolder.INSTANCE;
   }
}
