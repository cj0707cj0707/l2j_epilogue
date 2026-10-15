package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnCreatureSee implements IBaseEvent {
   private final Creature _creature;
   private final Creature _seen;

   public OnCreatureSee(Creature creature, Creature seen) {
      this._creature = creature;
      this._seen = seen;
   }

   public Creature getCreature() {
      return this._creature;
   }

   public Creature getSeen() {
      return this._seen;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_SEE;
   }
}
