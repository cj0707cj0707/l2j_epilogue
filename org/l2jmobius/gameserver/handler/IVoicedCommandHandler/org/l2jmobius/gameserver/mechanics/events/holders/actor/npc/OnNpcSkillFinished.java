package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OnNpcSkillFinished implements IBaseEvent {
   private final Npc _caster;
   private final Player _target;
   private final Skill _skill;

   public OnNpcSkillFinished(Npc caster, Player target, Skill skill) {
      this._caster = caster;
      this._target = target;
      this._skill = skill;
   }

   public Player getTarget() {
      return this._target;
   }

   public Npc getCaster() {
      return this._caster;
   }

   public Skill getSkill() {
      return this._skill;
   }

   @Override
   public EventType getType() {
      return EventType.ON_NPC_SKILL_FINISHED;
   }
}
