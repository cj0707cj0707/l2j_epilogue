package org.l2jmobius.gameserver.mechanics.events;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;

public class ListenersContainer {
   private Map<EventType, Queue<AbstractEventListener>> _listeners = null;

   public AbstractEventListener addListener(AbstractEventListener listener) {
      if (listener == null) {
         throw new NullPointerException("Listener cannot be null!");
      } else {
         this.getListeners().computeIfAbsent(listener.getType(), var0 -> new PriorityBlockingQueue<>()).add(listener);
         return listener;
      }
   }

   public AbstractEventListener removeListener(AbstractEventListener listener) {
      if (listener == null) {
         throw new NullPointerException("Listener cannot be null!");
      } else if (this._listeners == null) {
         throw new NullPointerException("Listeners container is not initialized!");
      } else {
         EventType type = listener.getType();
         Queue<AbstractEventListener> eventListenerQueue = this._listeners.get(type);
         if (eventListenerQueue == null) {
            throw new IllegalAccessError("Listeners container doesn't had " + type + " event type added!");
         } else {
            eventListenerQueue.remove(listener);
            if (eventListenerQueue.isEmpty()) {
               this._listeners.remove(type);
            }

            return listener;
         }
      }
   }

   public void removeListenerIf(EventType type, Predicate<? super AbstractEventListener> filter) {
      if (this._listeners != null) {
         for (AbstractEventListener listener : this.getListeners(type)) {
            if (filter.test(listener)) {
               listener.unregisterMe();
            }
         }
      }
   }

   public void removeListenerIf(Predicate<? super AbstractEventListener> filter) {
      if (this._listeners != null) {
         for (Queue<AbstractEventListener> queue : this.getListeners().values()) {
            for (AbstractEventListener listener : queue) {
               if (filter.test(listener)) {
                  listener.unregisterMe();
               }
            }
         }
      }
   }

   public boolean hasListener(EventType type) {
      if (this._listeners != null && this._listeners.containsKey(type)) {
         return true;
      } else {
         if (this instanceof Creature creature) {
            if (creature.getTemplate().hasListener(type)) {
               return true;
            }

            if (creature.isMonster()) {
               return Containers.Monsters().hasListener(type);
            }

            if (creature.isNpc()) {
               return Containers.Npcs().hasListener(type);
            }

            if (creature.isPlayer()) {
               return Containers.Players().hasListener(type);
            }
         }

         return false;
      }
   }

   public Collection<AbstractEventListener> getListeners(EventType type) {
      if (this._listeners != null) {
         Collection<AbstractEventListener> eventListenerQueue = this._listeners.get(type);
         if (eventListenerQueue != null) {
            return eventListenerQueue;
         }
      }

      return Collections.emptyList();
   }

   private Map<EventType, Queue<AbstractEventListener>> getListeners() {
      if (this._listeners == null) {
         synchronized (this) {
            if (this._listeners == null) {
               this._listeners = new ConcurrentHashMap<>();
            }
         }
      }

      return this._listeners;
   }
}
