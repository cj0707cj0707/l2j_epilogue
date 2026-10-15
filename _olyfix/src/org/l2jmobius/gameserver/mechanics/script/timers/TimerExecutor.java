package org.l2jmobius.gameserver.mechanics.script.timers;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

public class TimerExecutor<T> {
   private final Map<T, Set<TimerHolder<T>>> _timers = new ConcurrentHashMap<>();
   private final IEventTimerEvent<T> _eventListener;
   private final IEventTimerCancel<T> _cancelListener;

   public TimerExecutor(IEventTimerEvent<T> eventListener, IEventTimerCancel<T> cancelListener) {
      this._eventListener = eventListener;
      this._cancelListener = cancelListener;
   }

   private boolean addTimer(TimerHolder<T> holder) {
      Set<TimerHolder<T>> timers = this._timers.computeIfAbsent(holder.getEvent(), var0 -> ConcurrentHashMap.newKeySet());
      this.removeAndCancelTimers(timers, holder::isEqual);
      return timers.add(holder);
   }

   public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer) {
      return this.addTimer(new TimerHolder<>(event, params, time, npc, player, false, eventTimer, this._cancelListener, this));
   }

   public boolean addTimer(T event, long time, IEventTimerEvent<T> eventTimer) {
      return this.addTimer(new TimerHolder<>(event, null, time, null, null, false, eventTimer, this._cancelListener, this));
   }

   public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player) {
      return this.addTimer(event, params, time, npc, player, this._eventListener);
   }

   public boolean addTimer(T event, long time, Npc npc, Player player) {
      return this.addTimer(event, null, time, npc, player, this._eventListener);
   }

   private boolean addRepeatingTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer) {
      return this.addTimer(new TimerHolder<>(event, params, time, npc, player, true, eventTimer, this._cancelListener, this));
   }

   public boolean addRepeatingTimer(T event, long time, Npc npc, Player player) {
      return this.addRepeatingTimer(event, null, time, npc, player, this._eventListener);
   }

   public void onTimerPostExecute(TimerHolder<T> holder) {
      if (!holder.isRepeating()) {
         Set<TimerHolder<T>> timers = this._timers.get(holder.getEvent());
         if (timers == null || timers.isEmpty()) {
            return;
         }

         this.removeAndCancelTimers(timers, holder::isEqual);
         if (timers.isEmpty()) {
            this._timers.remove(holder.getEvent());
         }
      }
   }

   public void cancelAllTimers() {
      for (Set<TimerHolder<T>> set : this._timers.values()) {
         for (TimerHolder<T> timer : set) {
            timer.cancelTimer();
         }
      }

      this._timers.clear();
   }

   public boolean hasTimer(T event, Npc npc, Player player) {
      Set<TimerHolder<T>> timers = this._timers.get(event);
      if (timers != null && !timers.isEmpty()) {
         for (TimerHolder<T> holder : timers) {
            if (holder.isEqual(event, npc, player)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean cancelTimers(T event) {
      Set<TimerHolder<T>> timers = this._timers.remove(event);
      if (timers != null && !timers.isEmpty()) {
         timers.forEach(TimerHolder::cancelTimer);
         return true;
      } else {
         return false;
      }
   }

   public boolean cancelTimer(T event, Npc npc, Player player) {
      Set<TimerHolder<T>> timers = this._timers.get(event);
      if (timers != null && !timers.isEmpty()) {
         this.removeAndCancelTimers(timers, timer -> timer.isEqual(event, npc, player));
         return false;
      } else {
         return false;
      }
   }

   public void cancelTimersOf(Npc npc) {
      this.removeAndCancelTimers(timer -> timer.getNpc() == npc);
   }

   private void removeAndCancelTimers(Predicate<TimerHolder<T>> condition) {
      Objects.requireNonNull(condition);

      for (Set<TimerHolder<T>> timers : this._timers.values()) {
         this.removeAndCancelTimers(timers, condition);
      }
   }

   private void removeAndCancelTimers(Set<TimerHolder<T>> timers, Predicate<TimerHolder<T>> condition) {
      Objects.requireNonNull(timers);
      Objects.requireNonNull(condition);
      Iterator<TimerHolder<T>> it = timers.iterator();

      while (it.hasNext()) {
         TimerHolder<T> timer = it.next();
         if (condition.test(timer)) {
            it.remove();
            timer.cancelTimer();
         }
      }
   }
}
