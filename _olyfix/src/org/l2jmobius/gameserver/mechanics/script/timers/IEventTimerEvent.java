package org.l2jmobius.gameserver.mechanics.script.timers;

@FunctionalInterface
public interface IEventTimerEvent<T> {
   void onTimerEvent(TimerHolder<T> var1);
}
