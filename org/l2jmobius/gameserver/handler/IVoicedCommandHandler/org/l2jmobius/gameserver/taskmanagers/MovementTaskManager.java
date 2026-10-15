package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class MovementTaskManager {
   protected static final Logger LOGGER = Logger.getLogger(MovementTaskManager.class.getName());
   private static final Set<Set<Creature>> POOLS_CREATURE = ConcurrentHashMap.newKeySet();
   private static final Set<Set<Creature>> POOLS_PLAYER = ConcurrentHashMap.newKeySet();
   private static final int POOL_SIZE_CREATURE = 1000;
   private static final int POOL_SIZE_PLAYER = 500;
   private static final int TASK_DELAY_CREATURE = 100;
   private static final int TASK_DELAY_PLAYER = 50;

   protected MovementTaskManager() {
   }

   public synchronized void registerMovingObject(Creature creature) {
      if (creature.isPlayer()) {
         for (Set<Creature> pool : POOLS_PLAYER) {
            if (pool.contains(creature)) {
               return;
            }
         }

         for (Set<Creature> poolx : POOLS_PLAYER) {
            if (poolx.size() < 500) {
               poolx.add(creature);
               return;
            }
         }

         Set<Creature> poolxx = ConcurrentHashMap.newKeySet(500);
         poolxx.add(creature);
         ThreadPool.schedulePriorityTaskAtFixedRate(new MovementTaskManager.Movement(poolxx), 50L, 50L);
         POOLS_PLAYER.add(poolxx);
      } else {
         for (Set<Creature> poolxx : POOLS_CREATURE) {
            if (poolxx.contains(creature)) {
               return;
            }
         }

         for (Set<Creature> poolxxx : POOLS_CREATURE) {
            if (poolxxx.size() < 1000) {
               poolxxx.add(creature);
               return;
            }
         }

         Set<Creature> poolxxxx = ConcurrentHashMap.newKeySet(1000);
         poolxxxx.add(creature);
         ThreadPool.scheduleAtFixedRate(new MovementTaskManager.Movement(poolxxxx), 100L, 100L);
         POOLS_CREATURE.add(poolxxxx);
      }
   }

   public static MovementTaskManager getInstance() {
      return MovementTaskManager.SingletonHolder.INSTANCE;
   }

   private class Movement implements Runnable {
      private final Set<Creature> _creatures;

      public Movement(Set<Creature> creatures) {
         Objects.requireNonNull(MovementTaskManager.this);
         super();
         this._creatures = creatures;
      }

      @Override
      public void run() {
         if (!this._creatures.isEmpty()) {
            Iterator<Creature> iterator = this._creatures.iterator();

            while (iterator.hasNext()) {
               Creature creature = iterator.next();
               boolean removed = false;

               try {
                  if (creature.updatePosition()) {
                     iterator.remove();
                     removed = true;
                     creature.getAI().notifyActionArrived();
                  }
               } catch (Exception var5) {
                  if (!removed) {
                     iterator.remove();
                  }

                  MovementTaskManager.LOGGER.warning("MovementTaskManager: Problem updating position of " + creature);
                  MovementTaskManager.LOGGER.warning(TraceUtil.getStackTrace(var5));
               }
            }
         }
      }
   }

   private static class SingletonHolder {
      protected static final MovementTaskManager INSTANCE = new MovementTaskManager();
   }
}
