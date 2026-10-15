package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnCreatureKilled implements IBaseEvent {
   private final Creature _attacker;
   private final Creature _target;

   public OnCreatureKilled(Creature attacker, Creature target) {
      this._attacker = attacker;
      this._target = target;
   }

   public Creature getAttacker() {
      return this._attacker;
   }

   public Creature getTarget() {
      return this._target;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_KILLED;
   }
}
