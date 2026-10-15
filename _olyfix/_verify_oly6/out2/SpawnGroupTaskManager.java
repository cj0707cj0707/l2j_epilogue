package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroupEntry;

public class SpawnGroupTaskManager implements Runnable {
   private static final Queue<org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager.PendingGroupSpawn> PENDING_SPAWNS = new ConcurrentLinkedQueue<>();
   private static boolean _working = false;

   protected SpawnGroupTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 0L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PENDING_SPAWNS.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager.PendingGroupSpawn> iterator = PENDING_SPAWNS.iterator();

            while (iterator.hasNext()) {
               org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager.PendingGroupSpawn pending = iterator.next();
               if (currentTime > pending._time) {
                  iterator.remove();
                  pending._group.spawnUnits(pending._entry, 1);
               }
            }
         }

         _working = false;
      }
   }

   public void add(SpawnGroup group, SpawnGroupEntry entry, long time) {
      PENDING_SPAWNS.add(new org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager.PendingGroupSpawn(group, entry, time));
   }

   public void clear() {
      PENDING_SPAWNS.clear();
   }

   public static org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager.SingletonHolder.INSTANCE;
   }

   private static class PendingGroupSpawn {
      final SpawnGroup _group;
      final SpawnGroupEntry _entry;
      final long _time;

      PendingGroupSpawn(SpawnGroup group, SpawnGroupEntry entry, long time) {
         this._group = group;
         this._entry = entry;
         this._time = time;
      }
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager();
   }
}
