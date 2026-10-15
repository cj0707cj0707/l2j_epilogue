package org.l2jmobius.gameserver.mechanics.events.listeners;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

public class FunctionEventListener extends AbstractEventListener {
   private static final Logger LOGGER = Logger.getLogger(FunctionEventListener.class.getName());
   private final Function<IBaseEvent, ? extends AbstractEventReturn> _callback;

   public FunctionEventListener(
      ListenersContainer container, EventType type, Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, Object owner
   ) {
      super(container, type, owner);
      this._callback = callback;
   }

   @Override
   public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass) {
      try {
         return returnBackClass.cast(this._callback.apply(event));
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while invoking " + event + " on " + this.getOwner(), (Throwable)var4);
         return null;
      }
   }
}
