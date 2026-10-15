package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnCreatureTeleported implements IBaseEvent {
   private final Creature _creature;

   public OnCreatureTeleported(Creature creature) {
      this._creature = creature;
   }

   public Creature getCreature() {
      return this._creature;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_TELEPORTED;
   }
}
