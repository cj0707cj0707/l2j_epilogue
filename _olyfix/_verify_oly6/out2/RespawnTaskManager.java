package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

public class RespawnTaskManager implements Runnable {
   private static final Map<Npc, Long> PENDING_RESPAWNS = new ConcurrentHashMap<>();
   private static boolean _working = false;

   protected RespawnTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 0L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PENDING_RESPAWNS.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<Npc, Long>> iterator = PENDING_RESPAWNS.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Npc, Long> entry = iterator.next();
               if (currentTime > entry.getValue()) {
                  iterator.remove();
                  Npc npc = entry.getKey();
                  Spawn spawn = npc.getSpawn();
                  if (spawn != null) {
                     spawn.respawnNpc(npc);
                     spawn._scheduledCount--;
                  }
               }
            }
         }

         _working = false;
      }
   }

   public void add(Npc npc, long time) {
      PENDING_RESPAWNS.put(npc, time);
   }

   public static org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager();
   }
}
