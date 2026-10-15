package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.List;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class Monster extends Attackable {
   protected boolean _enableMinions = true;
   private Monster _master = null;
   private MinionList _minionList = null;

   public Monster(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Monster);
      this.setAutoAttackable(true);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (!this.isFakePlayer()) {
         if (NpcConfig.GUARD_ATTACK_AGGRO_MOB && this.isAggressive() && attacker instanceof Guard) {
            return true;
         } else if (attacker.isMonster()) {
            return attacker.isFakePlayer();
         } else {
            return !attacker.isPlayable() && !attacker.isAttackable() && !(attacker instanceof Trap) && !(attacker instanceof EffectPoint)
               ? false
               : super.isAutoAttackable(attacker);
         }
      } else {
         return FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE || this.isInCombat() || attacker.isMonster() || this.getScriptValue() > 0;
      }
   }

   @Override
   public boolean isAggressive() {
      return this.getTemplate().isAggressive() && !this.isAffected(EffectFlag.PASSIVE);
   }

   @Override
   public void onSpawn() {
      if (!this.isTeleporting() && this._master != null) {
         this.setRandomWalking(false);
         this.setIsRaidMinion(this._master.isRaid());
         this._master.getMinionList().onMinionSpawn(this);
      }

      super.onSpawn();
   }

   @Override
   public synchronized void onTeleported() {
      super.onTeleported();
      if (this.hasMinions()) {
         this.getMinionList().onMasterTeleported();
      }
   }

   @Override
   public boolean deleteMe() {
      if (this.hasMinions()) {
         this.getMinionList().onMasterDie(true);
      }

      if (this._master != null) {
         this._master.getMinionList().onMinionDie(this, 0);
      }

      return super.deleteMe();
   }

   public Monster getLeader() {
      return this._master;
   }

   public void setLeader(Monster leader) {
      this._master = leader;
   }

   public void enableMinions(boolean value) {
      this._enableMinions = value;
   }

   public boolean hasMinions() {
      return this._minionList != null;
   }

   public MinionList getMinionList() {
      if (this._minionList == null) {
         synchronized (this) {
            if (this._minionList == null) {
               this._minionList = new MinionList(this);
            }
         }
      }

      return this._minionList;
   }

   @Override
   public boolean isMonster() {
      return true;
   }

   @Override
   public Monster asMonster() {
      return this;
   }

   @Override
   public boolean isWalker() {
      return this._master == null ? super.isWalker() : this._master.isWalker();
   }

   @Override
   public boolean giveRaidCurse() {
      return this.isRaidMinion() && this._master != null ? this._master.giveRaidCurse() : super.giveRaidCurse();
   }

   @Override
   public void doCast(Skill skill, Creature target, List<WorldObject> targets) {
      if (!skill.hasNegativeEffect() && this.getTarget() != null && this.getTarget().isPlayer()) {
         this.setCastingNow(false);
         this.setCastingSimultaneouslyNow(false);
      } else {
         super.doCast(skill, target, targets);
      }
   }
}
