package org.l2jmobius.gameserver.mechanics.events.listeners;

import java.util.function.Consumer;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

public class ConsumerEventListener extends AbstractEventListener {
   private final Consumer<IBaseEvent> _callback;

   public ConsumerEventListener(ListenersContainer container, EventType type, Consumer<? extends IBaseEvent> callback, Object owner) {
      super(container, type, owner);
      this._callback = callback;
   }

   @Override
   public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass) {
      this._callback.accept(event);
      return null;
   }
}
