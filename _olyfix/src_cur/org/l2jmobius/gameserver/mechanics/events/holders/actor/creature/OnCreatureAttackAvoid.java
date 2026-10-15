package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnCreatureAttackAvoid implements IBaseEvent {
   private Creature _attacker;
   private Creature _target;
   private boolean _damageOverTime;

   public Creature getAttacker() {
      return this._attacker;
   }

   public synchronized void setAttacker(Creature attacker) {
      this._attacker = attacker;
   }

   public Creature getTarget() {
      return this._target;
   }

   public synchronized void setTarget(Creature target) {
      this._target = target;
   }

   public boolean isDamageOverTime() {
      return this._damageOverTime;
   }

   public synchronized void setDamageOverTime(boolean damageOverTime) {
      this._damageOverTime = damageOverTime;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_ATTACK_AVOID;
   }
}
