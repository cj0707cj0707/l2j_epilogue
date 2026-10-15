package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import java.util.List;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OnNpcSkillSee implements IBaseEvent {
   private final Npc _npc;
   private final Player _caster;
   private final Skill _skill;
   private final List<WorldObject> _targets;
   private final boolean _isSummon;

   public OnNpcSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon) {
      this._npc = npc;
      this._caster = caster;
      this._skill = skill;
      this._targets = targets;
      this._isSummon = isSummon;
   }

   public Npc getTarget() {
      return this._npc;
   }

   public Player getCaster() {
      return this._caster;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public List<WorldObject> getTargets() {
      return this._targets;
   }

   public boolean isSummon() {
      return this._isSummon;
   }

   @Override
   public EventType getType() {
      return EventType.ON_NPC_SKILL_SEE;
   }
}
