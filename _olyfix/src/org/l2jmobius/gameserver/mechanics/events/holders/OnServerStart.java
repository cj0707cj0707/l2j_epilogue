package org.l2jmobius.gameserver.mechanics.events.holders;

import org.l2jmobius.gameserver.mechanics.events.EventType;

public class OnServerStart implements IBaseEvent {
   @Override
   public EventType getType() {
      return EventType.ON_SERVER_START;
   }
}
