package org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.siege.Siege;

public class OnCastleSiegeStart implements IBaseEvent {
   private final Siege _siege;

   public OnCastleSiegeStart(Siege siege) {
      this._siege = siege;
   }

   public Siege getSiege() {
      return this._siege;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CASTLE_SIEGE_START;
   }
}
