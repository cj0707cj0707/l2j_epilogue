package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class DecayTaskManager implements Runnable {
   private static final Map<Creature, Long> DECAY_SCHEDULES = new ConcurrentHashMap<>();
   private static boolean _working = false;

   protected DecayTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 0L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!DECAY_SCHEDULES.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<Creature, Long>> iterator = DECAY_SCHEDULES.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Creature, Long> entry = iterator.next();
               if (currentTime > entry.getValue()) {
                  entry.getKey().onDecay();
                  iterator.remove();
               }
            }
         }

         _working = false;
      }
   }

   public void add(Creature creature) {
      if (creature != null) {
         long delay;
         if (creature.getTemplate() instanceof NpcTemplate) {
            delay = ((NpcTemplate)creature.getTemplate()).getCorpseTime();
         } else {
            delay = NpcConfig.DEFAULT_CORPSE_TIME;
         }

         if (creature.isAttackable() && (creature.asAttackable().isSpoiled() || creature.asAttackable().isSeeded())) {
            delay += NpcConfig.SPOILED_CORPSE_EXTEND_TIME;
         }

         if (creature.isPlayer()) {
            Player player = creature.asPlayer();
            if (player.isOfflinePlay() && OfflinePlayConfig.OFFLINE_PLAY_LOGOUT_ON_DEATH) {
               delay = 10L;
            } else if (PlayerConfig.DISCONNECT_AFTER_DEATH) {
               delay = 3600L;
            }
         }

         DECAY_SCHEDULES.put(creature, System.currentTimeMillis() + delay * 1000L);
      }
   }

   public void cancel(Creature creature) {
      DECAY_SCHEDULES.remove(creature);
   }

   public long getRemainingTime(Creature creature) {
      Long time = DECAY_SCHEDULES.get(creature);
      return time != null ? time - System.currentTimeMillis() : Long.MAX_VALUE;
   }

   @Override
   public String toString() {
      StringBuilder ret = new StringBuilder();
      ret.append("============= DecayTask Manager Report ============");
      ret.append(System.lineSeparator());
      ret.append("Tasks count: ");
      ret.append(DECAY_SCHEDULES.size());
      ret.append(System.lineSeparator());
      ret.append("Tasks dump:");
      ret.append(System.lineSeparator());
      long time = System.currentTimeMillis();

      for (Entry<Creature, Long> entry : DECAY_SCHEDULES.entrySet()) {
         ret.append("Class/Name: ");
         ret.append(entry.getKey().getClass().getSimpleName());
         ret.append('/');
         ret.append(entry.getKey().getName());
         ret.append(" decay timer: ");
         ret.append(entry.getValue() - time);
         ret.append(System.lineSeparator());
      }

      return ret.toString();
   }

   public static org.l2jmobius.gameserver.taskmanagers.DecayTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.DecayTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.DecayTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.DecayTaskManager();
   }
}
