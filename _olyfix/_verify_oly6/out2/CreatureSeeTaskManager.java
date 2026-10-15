package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class CreatureSeeTaskManager implements Runnable {
   private static final Set<Creature> CREATURES = ConcurrentHashMap.newKeySet();
   private static boolean _working = false;

   protected CreatureSeeTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 1000L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;

         for (Creature creature : CREATURES) {
            creature.updateSeenCreatures();
         }

         _working = false;
      }
   }

   public void add(Creature creature) {
      CREATURES.add(creature);
   }

   public void remove(Creature creature) {
      CREATURES.remove(creature);
   }

   public static org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager();
   }
}
