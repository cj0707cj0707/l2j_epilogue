package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.entity.actor.Attackable;

public class AttackableThinkTaskManager {
   private static final Set<Set<Attackable>> POOLS = ConcurrentHashMap.newKeySet();
   private static final int POOL_SIZE = 1000;
   private static final int TASK_DELAY = 1000;

   protected AttackableThinkTaskManager() {
   }

   public synchronized void add(Attackable attackable) {
      for (Set<Attackable> pool : POOLS) {
         if (pool.contains(attackable)) {
            return;
         }
      }

      for (Set<Attackable> poolx : POOLS) {
         if (poolx.size() < 1000) {
            poolx.add(attackable);
            return;
         }
      }

      Set<Attackable> poolxx = ConcurrentHashMap.newKeySet(1000);
      poolxx.add(attackable);
      ThreadPool.schedulePriorityTaskAtFixedRate(new org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager.AttackableThink(poolxx), 1000L, 1000L);
      POOLS.add(poolxx);
   }

   public void remove(Attackable attackable) {
      for (Set<Attackable> pool : POOLS) {
         if (pool.remove(attackable)) {
            return;
         }
      }
   }

   public static org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager.SingletonHolder.INSTANCE;
   }

   private class AttackableThink implements Runnable {
      private final Set<Attackable> _attackables;

      public AttackableThink(Set<Attackable> attackables) {
         Objects.requireNonNull(AttackableThinkTaskManager.this);
         super();
         this._attackables = attackables;
      }

      @Override
      public void run() {
         if (!this._attackables.isEmpty()) {
            Iterator<Attackable> iterator = this._attackables.iterator();

            while (iterator.hasNext()) {
               Attackable attackable = iterator.next();
               if (attackable.hasAI()) {
                  CreatureAI ai = attackable.getAI();
                  if (ai != null) {
                     ai.notifyActionThink();
                  } else {
                     iterator.remove();
                  }
               } else {
                  iterator.remove();
               }
            }
         }
      }
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager();
   }
}
