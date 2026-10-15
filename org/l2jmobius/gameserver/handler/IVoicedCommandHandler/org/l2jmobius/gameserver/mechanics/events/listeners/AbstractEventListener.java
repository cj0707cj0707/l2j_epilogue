package org.l2jmobius.gameserver.mechanics.events.listeners;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

public abstract class AbstractEventListener implements Comparable<AbstractEventListener> {
   private int _priority = 0;
   private final ListenersContainer _container;
   private final EventType _type;
   private final Object _owner;

   public AbstractEventListener(ListenersContainer container, EventType type, Object owner) {
      this._container = container;
      this._type = type;
      this._owner = owner;
   }

   public ListenersContainer getContainer() {
      return this._container;
   }

   public EventType getType() {
      return this._type;
   }

   public Object getOwner() {
      return this._owner;
   }

   public int getPriority() {
      return this._priority;
   }

   public void setPriority(int priority) {
      this._priority = priority;
   }

   public abstract <R extends AbstractEventReturn> R executeEvent(IBaseEvent var1, Class<R> var2);

   public void unregisterMe() {
      this._container.removeListener(this);
   }

   public int compareTo(AbstractEventListener o) {
      return Integer.compare(o.getPriority(), this.getPriority());
   }
}
