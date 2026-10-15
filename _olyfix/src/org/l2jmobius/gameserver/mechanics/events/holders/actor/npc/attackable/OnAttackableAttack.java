package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OnAttackableAttack implements IBaseEvent {
   private final Player _attacker;
   private final Attackable _target;
   private final int _damage;
   private final Skill _skill;
   private final boolean _isSummon;

   public OnAttackableAttack(Player attacker, Attackable target, int damage, Skill skill, boolean isSummon) {
      this._attacker = attacker;
      this._target = target;
      this._damage = damage;
      this._skill = skill;
      this._isSummon = isSummon;
   }

   public Player getAttacker() {
      return this._attacker;
   }

   public Attackable getTarget() {
      return this._target;
   }

   public int getDamage() {
      return this._damage;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public boolean isSummon() {
      return this._isSummon;
   }

   @Override
   public EventType getType() {
      return EventType.ON_ATTACKABLE_ATTACK;
   }
}
