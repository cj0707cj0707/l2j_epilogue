package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class QuestGuard extends Guard {
   private boolean _isAutoAttackable = true;
   private boolean _isPassive = false;

   public QuestGuard(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.QuestGuard);
      this.setCanReturnToSpawnPoint(false);
   }

   @Override
   public void addDamage(Creature attacker, int damage, Skill skill) {
      super.addDamage(attacker, damage, skill);
      if (attacker.isAttackable() && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(null, this, damage, skill, false), this);
      }
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (killer.isAttackable() && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this)) {
            EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(null, this, false), this, this._onKillDelay);
         }

         return true;
      }
   }

   @Override
   public void addDamageHate(Creature attacker, long damage, long aggro) {
      if (!this._isPassive && !attacker.isPlayer()) {
         super.addDamageHate(attacker, damage, aggro);
      }
   }

   public void setPassive(boolean value) {
      this._isPassive = value;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return this._isAutoAttackable && !attacker.isPlayer();
   }

   @Override
   public void setAutoAttackable(boolean value) {
      this._isAutoAttackable = value;
   }

   public boolean isPassive() {
      return this._isPassive;
   }
}
