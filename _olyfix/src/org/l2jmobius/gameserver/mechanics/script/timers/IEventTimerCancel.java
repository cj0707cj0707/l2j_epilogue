package org.l2jmobius.gameserver.mechanics.script.timers;

@FunctionalInterface
public interface IEventTimerCancel<T> {
   void onTimerCancel(TimerHolder<T> var1);
}
