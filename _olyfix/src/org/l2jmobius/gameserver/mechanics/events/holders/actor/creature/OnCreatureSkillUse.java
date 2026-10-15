package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import java.util.Collection;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OnCreatureSkillUse implements IBaseEvent {
   private Creature _caster;
   private Skill _skill;
   private boolean _simultaneously;
   private Creature _target;
   private Collection<WorldObject> _targets;

   public Creature getCaster() {
      return this._caster;
   }

   public synchronized void setCaster(Creature caster) {
      this._caster = caster;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public synchronized void setSkill(Skill skill) {
      this._skill = skill;
   }

   public boolean isSimultaneously() {
      return this._simultaneously;
   }

   public synchronized void setSimultaneously(boolean simultaneously) {
      this._simultaneously = simultaneously;
   }

   public Creature getTarget() {
      return this._target;
   }

   public synchronized void setTarget(Creature target) {
      this._target = target;
   }

   public Collection<WorldObject> getTargets() {
      return this._targets;
   }

   public synchronized void setTargets(Collection<WorldObject> targets) {
      this._targets = targets;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_SKILL_USE;
   }
}
