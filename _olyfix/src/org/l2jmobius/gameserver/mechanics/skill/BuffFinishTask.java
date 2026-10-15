package org.l2jmobius.gameserver.mechanics.skill;

import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

public class BuffFinishTask {
   private final Map<BuffInfo, AtomicInteger> _buffInfos = new ConcurrentHashMap<>();
   private ScheduledFuture<?> _task = null;
   private boolean _stopped = false;

   public synchronized void removeBuffInfo(BuffInfo info) {
      this._buffInfos.remove(info);
      if (this._buffInfos.isEmpty() && this._task != null) {
         this._task.cancel(true);
         this._task = null;
      }
   }

   public synchronized void addBuffInfo(BuffInfo info) {
      this._buffInfos.put(info, new AtomicInteger());
      if (this._task == null && !this._stopped) {
         this._task = ThreadPool.scheduleAtFixedRate(new BuffFinishTask.BuffFinishRunnable(), 0L, 1000L);
      }
   }

   public synchronized void start() {
      this._stopped = false;
      if (!this._buffInfos.isEmpty() && this._task == null) {
         this._task = ThreadPool.scheduleAtFixedRate(new BuffFinishTask.BuffFinishRunnable(), 0L, 1000L);
      }
   }

   public synchronized void stop() {
      this._stopped = true;
      if (this._task != null) {
         this._task.cancel(true);
      }
   }

   private class BuffFinishRunnable implements Runnable {
      private BuffFinishRunnable() {
         Objects.requireNonNull(BuffFinishTask.this);
         super();
      }

      @Override
      public void run() {
         for (Entry<BuffInfo, AtomicInteger> entry : BuffFinishTask.this._buffInfos.entrySet()) {
            BuffInfo info = entry.getKey();
            Creature effected = info.getEffected();
            if (effected != null && entry.getValue().incrementAndGet() > info.getAbnormalTime()) {
               ThreadPool.execute(() -> effected.getEffectList().stopSkillEffects(SkillFinishType.NORMAL, info.getSkill().getId()));
            }
         }
      }
   }
}
