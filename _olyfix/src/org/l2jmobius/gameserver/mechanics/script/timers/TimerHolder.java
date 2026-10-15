package org.l2jmobius.gameserver.mechanics.script.timers;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

public class TimerHolder<T> implements Runnable {
   private final T _event;
   private final StatSet _params;
   private final long _time;
   private final Npc _npc;
   private final Player _player;
   private final boolean _isRepeating;
   private final IEventTimerEvent<T> _eventScript;
   private final IEventTimerCancel<T> _cancelScript;
   private final TimerExecutor<T> _postExecutor;
   private final ScheduledFuture<?> _task;

   public TimerHolder(
      T event,
      StatSet params,
      long time,
      Npc npc,
      Player player,
      boolean isRepeating,
      IEventTimerEvent<T> eventScript,
      IEventTimerCancel<T> cancelScript,
      TimerExecutor<T> postExecutor
   ) {
      Objects.requireNonNull(event, this.getClass().getSimpleName() + ": \"event\" cannot be null!");
      Objects.requireNonNull(eventScript, this.getClass().getSimpleName() + ": \"script\" cannot be null!");
      Objects.requireNonNull(postExecutor, this.getClass().getSimpleName() + ": \"postExecutor\" cannot be null!");
      this._event = event;
      this._params = params;
      this._time = time;
      this._npc = npc;
      this._player = player;
      this._isRepeating = isRepeating;
      this._eventScript = eventScript;
      this._cancelScript = cancelScript;
      this._postExecutor = postExecutor;
      this._task = isRepeating ? ThreadPool.scheduleAtFixedRate(this, this._time, this._time) : ThreadPool.schedule(this, this._time);
      if (npc != null) {
         npc.addTimerHolder(this);
      }

      if (player != null) {
         player.addTimerHolder(this);
      }
   }

   public T getEvent() {
      return this._event;
   }

   public StatSet getParams() {
      return this._params;
   }

   public Npc getNpc() {
      return this._npc;
   }

   public Player getPlayer() {
      return this._player;
   }

   public boolean isRepeating() {
      return this._isRepeating;
   }

   public void cancelTimer() {
      if (this._npc != null) {
         this._npc.removeTimerHolder(this);
      }

      if (this._player != null) {
         this._player.removeTimerHolder(this);
      }

      if (this._task != null && !this._task.isCancelled() && !this._task.isDone()) {
         this._task.cancel(false);
         this._cancelScript.onTimerCancel(this);
      }
   }

   public void cancelTask() {
      if (this._task != null && !this._task.isDone() && !this._task.isCancelled()) {
         this._task.cancel(false);
      }
   }

   public long getRemainingTime() {
      return this._task != null && !this._task.isCancelled() && !this._task.isDone() ? this._task.getDelay(TimeUnit.MILLISECONDS) : -1L;
   }

   public boolean isEqual(T event, Npc npc, Player player) {
      return this._event.equals(event) && this._npc == npc && this._player == player;
   }

   public boolean isEqual(TimerHolder<T> timer) {
      return this._event.equals(timer._event) && this._npc == timer._npc && this._player == timer._player;
   }

   @Override
   public void run() {
      this._postExecutor.onTimerPostExecute(this);
      this._eventScript.onTimerEvent(this);
   }

   @Override
   public String toString() {
      return "event: "
         + this._event
         + " params: "
         + this._params
         + " time: "
         + this._time
         + " npc: "
         + this._npc
         + " player: "
         + this._player
         + " repeating: "
         + this._isRepeating
         + " script: "
         + this._eventScript.getClass().getSimpleName()
         + " postExecutor: "
         + this._postExecutor.getClass().getSimpleName();
   }
}
