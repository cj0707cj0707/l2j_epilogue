package org.l2jmobius.gameserver.mechanics.events.holders.actor.trap;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnTrapAction implements IBaseEvent {
   private final Trap _trap;
   private final Creature _trigger;
   private final TrapAction _action;

   public OnTrapAction(Trap trap, Creature trigger, TrapAction action) {
      this._trap = trap;
      this._trigger = trigger;
      this._action = action;
   }

   public Trap getTrap() {
      return this._trap;
   }

   public Creature getTrigger() {
      return this._trigger;
   }

   public TrapAction getAction() {
      return this._action;
   }

   @Override
   public EventType getType() {
      return EventType.ON_TRAP_ACTION;
   }
}
