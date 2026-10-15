package org.l2jmobius.gameserver.mechanics.events.holders;

import org.l2jmobius.gameserver.mechanics.events.EventType;

public class OnDayNightChange implements IBaseEvent {
   private final boolean _isNight;

   public OnDayNightChange(boolean isNight) {
      this._isNight = isNight;
   }

   public boolean isNight() {
      return this._isNight;
   }

   @Override
   public EventType getType() {
      return EventType.ON_DAY_NIGHT_CHANGE;
   }
}
