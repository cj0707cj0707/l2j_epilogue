package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnAttackableKill implements IBaseEvent {
   private final Player _attacker;
   private final Attackable _target;
   private final boolean _isSummon;

   public OnAttackableKill(Player attacker, Attackable target, boolean isSummon) {
      this._attacker = attacker;
      this._target = target;
      this._isSummon = isSummon;
   }

   public Player getAttacker() {
      return this._attacker;
   }

   public Attackable getTarget() {
      return this._target;
   }

   public boolean isSummon() {
      return this._isSummon;
   }

   @Override
   public EventType getType() {
      return EventType.ON_ATTACKABLE_KILL;
   }
}
