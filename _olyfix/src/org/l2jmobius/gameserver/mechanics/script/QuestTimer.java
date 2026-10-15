package org.l2jmobius.gameserver.mechanics.script;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;

public class QuestTimer {
   protected final String _name;
   protected final Quest _quest;
   protected final Npc _npc;
   protected final Player _player;
   protected final boolean _isRepeating;
   protected ScheduledFuture<?> _scheduler;

   public QuestTimer(Quest quest, String name, long time, Npc npc, Player player, boolean repeating) {
      this._quest = quest;
      this._name = name;
      this._npc = npc;
      this._player = player;
      this._isRepeating = repeating;
      if (repeating) {
         this._scheduler = ThreadPool.scheduleAtFixedRate(new QuestTimer.ScheduleTimerTask(), time, time);
      } else {
         this._scheduler = ThreadPool.schedule(new QuestTimer.ScheduleTimerTask(), time);
      }

      if (npc != null) {
         npc.addQuestTimer(this);
      }

      if (player != null) {
         player.addQuestTimer(this);
      }
   }

   public void cancel() {
      this.cancelTask();
      if (this._npc != null) {
         this._npc.removeQuestTimer(this);
      }

      if (this._player != null) {
         this._player.removeQuestTimer(this);
      }
   }

   public void cancelTask() {
      if (this._scheduler != null && !this._scheduler.isDone() && !this._scheduler.isCancelled()) {
         this._scheduler.cancel(false);
         this._scheduler = null;
      }

      this._quest.removeQuestTimer(this);
   }

   public boolean equals(Quest quest, String name, Npc npc, Player player) {
      if (quest == null || quest != this._quest) {
         return false;
      } else {
         return name != null && name.equals(this._name) ? npc == this._npc && player == this._player : false;
      }
   }

   public boolean isActive() {
      return this._scheduler != null && !this._scheduler.isCancelled() && !this._scheduler.isDone();
   }

   public boolean isRepeating() {
      return this._isRepeating;
   }

   public Quest getQuest() {
      return this._quest;
   }

   public Npc getNpc() {
      return this._npc;
   }

   public Player getPlayer() {
      return this._player;
   }

   @Override
   public String toString() {
      return this._name;
   }

   public class ScheduleTimerTask implements Runnable {
      public ScheduleTimerTask() {
         Objects.requireNonNull(QuestTimer.this);
         super();
      }

      @Override
      public void run() {
         if (QuestTimer.this._scheduler != null) {
            if (!QuestTimer.this._isRepeating) {
               QuestTimer.this.cancel();
            }

            QuestTimer.this._quest.notifyEvent(QuestTimer.this._name, QuestTimer.this._npc, QuestTimer.this._player);
         }
      }
   }
}
