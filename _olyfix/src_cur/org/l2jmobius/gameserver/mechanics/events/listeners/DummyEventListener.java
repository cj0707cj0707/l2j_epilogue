package org.l2jmobius.gameserver.mechanics.events.listeners;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

public class DummyEventListener extends AbstractEventListener {
   public DummyEventListener(ListenersContainer container, EventType type, Object owner) {
      super(container, type, owner);
   }

   @Override
   public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass) {
      return null;
   }
}
