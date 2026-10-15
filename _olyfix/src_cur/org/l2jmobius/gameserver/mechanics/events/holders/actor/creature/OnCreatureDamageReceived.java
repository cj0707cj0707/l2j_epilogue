package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OnCreatureDamageReceived implements IBaseEvent {
   private Creature _attacker;
   private Creature _target;
   private double _damage;
   private Skill _skill;
   private boolean _crit;
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

   public double getDamage() {
      return this._damage;
   }

   public synchronized void setDamage(double damage) {
      this._damage = damage;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public synchronized void setSkill(Skill skill) {
      this._skill = skill;
   }

   public boolean isCritical() {
      return this._crit;
   }

   public synchronized void setCritical(boolean crit) {
      this._crit = crit;
   }

   public boolean isDamageOverTime() {
      return this._damageOverTime;
   }

   public synchronized void setDamageOverTime(boolean damageOverTime) {
      this._damageOverTime = damageOverTime;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_DAMAGE_RECEIVED;
   }
}
