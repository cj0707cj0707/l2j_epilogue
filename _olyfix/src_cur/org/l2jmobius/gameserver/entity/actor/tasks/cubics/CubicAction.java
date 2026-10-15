package org.l2jmobius.gameserver.entity.actor.tasks.cubics;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.CubicTargetType;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class CubicAction implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(CubicAction.class.getName());
   private final Cubic _cubic;
   private final AtomicInteger _currentCount = new AtomicInteger();
   private final int _chance;

   public CubicAction(Cubic cubic, int chance) {
      this._cubic = cubic;
      this._chance = chance;
   }

   @Override
   public void run() {
      if (this._cubic != null) {
         if (!this._cubic.getOwner().isDead() && this._cubic.getOwner().isOnline()) {
            try {
               if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._cubic.getOwner())) {
                  if (!this._cubic.getOwner().hasSummon()) {
                     this._cubic.stopAction();
                     return;
                  }

                  if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._cubic.getOwner().getSummon())) {
                     this._cubic.stopAction();
                     return;
                  }
               }

               if (this._cubic.getCubicMaxCount() > -1 && this._currentCount.get() >= this._cubic.getCubicMaxCount()) {
                  this._cubic.stopAction();
                  if (this._cubic.getTemplate() != null && this._cubic.getTemplate().getUseUp() > 0) {
                     this._cubic.getOwner().removeCubic(this._cubic.getId());
                     this._cubic.getOwner().broadcastUserInfo();
                  }

                  return;
               }

               if (this.tryUsePrioritySkill()) {
                  this._currentCount.incrementAndGet();
                  return;
               }

               if (Rnd.get(100) >= this._chance) {
                  return;
               }

               if (this.tryUseSkill(this._cubic.getCubicSkill())) {
                  this._currentCount.incrementAndGet();
               }
            } catch (Exception var2) {
               LOGGER.log(
                  Level.SEVERE, "Cubic action failed. cubicId: " + this._cubic.getId() + " ownerId: " + this._cubic.getOwner().getObjectId(), (Throwable)var2
               );
            }
         } else {
            this._cubic.getOwner().removeCubic(this._cubic.getId());
            this._cubic.getOwner().broadcastUserInfo();
         }
      }
   }

   private boolean tryUsePrioritySkill() {
      for (CubicSkill cubicSkill : this._cubic.getCubicSkills()) {
         if (cubicSkill.isPriority() && this.tryUseSkill(cubicSkill)) {
            return true;
         }
      }

      return false;
   }

   private boolean tryUseSkill(CubicSkill cubicSkill) {
      if (cubicSkill == null) {
         return false;
      } else {
         Skill skill = cubicSkill.getSkill();
         if (skill == null) {
            return false;
         } else {
            Creature target = this.resolveTarget(cubicSkill);
            if (target != null && !target.isDead()) {
               if (this._cubic.getTemplate() != null && !this._cubic.getTemplate().validateConditions(this._cubic, this._cubic.getOwner(), target)) {
                  return false;
               } else if (!cubicSkill.validateConditions(this._cubic, this._cubic.getOwner(), target)) {
                  return false;
               } else {
                  this.activateSkill(skill, target);
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   private Creature resolveTarget(CubicSkill cubicSkill) {
      CubicTargetType targetType = this._cubic.getTemplate() != null ? this._cubic.getTemplate().getTargetType() : CubicTargetType.TARGET;
      if (targetType == CubicTargetType.BY_SKILL) {
         targetType = cubicSkill.getTargetType();
      }

      switch (targetType) {
         case HEAL:
            this._cubic.cubicTargetForHeal();
            return this._cubic.getTarget();
         case MASTER:
            this._cubic.setTarget(this._cubic.getOwner());
            return this._cubic.getOwner();
         case TARGET:
         default:
            this._cubic.getCubicTarget();
            if (!Cubic.isInCubicRange(this._cubic.getOwner(), this._cubic.getTarget())) {
               this._cubic.setTarget(null);
            }

            return this._cubic.getTarget();
      }
   }

   private void activateSkill(Skill skill, Creature target) {
      this._cubic.getOwner().broadcastSkillPacket(new MagicSkillUse(this._cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0), target);
      if (skill.isContinuous()) {
         this._cubic.useCubicContinuous(skill, Collections.singletonList(target));
      } else if (skill.hasEffectType(EffectType.MAGICAL_ATTACK)) {
         this._cubic.useCubicMdam(skill, Collections.singletonList(target));
      } else if (skill.hasEffectType(EffectType.HP_DRAIN)) {
         this._cubic.useCubicDrain(skill, Collections.singletonList(target));
      } else if (skill.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE)) {
         this._cubic.useCubicDisabler(skill, Collections.singletonList(target));
      } else if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT)) {
         this._cubic.useCubicContinuous(skill, Collections.singletonList(target));
      } else if (skill.hasEffectType(EffectType.AGGRESSION)) {
         this._cubic.useCubicDisabler(skill, Collections.singletonList(target));
      } else {
         skill.activateSkill(this._cubic, Collections.singletonList(target));
      }
   }
}
