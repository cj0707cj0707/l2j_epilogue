package org.l2jmobius.gameserver.mechanics.events.holders.sieges.fort;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;

public class OnFortSiegeFinish implements IBaseEvent {
   private final FortSiege _siege;

   public OnFortSiegeFinish(FortSiege siege) {
      this._siege = siege;
   }

   public FortSiege getSiege() {
      return this._siege;
   }

   @Override
   public EventType getType() {
      return EventType.ON_FORT_SIEGE_FINISH;
   }
}
