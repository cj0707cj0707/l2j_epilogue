package org.l2jmobius.gameserver.ai;

import java.util.Collection;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class FortSiegeGuardAI extends CreatureAI implements Runnable {
   private static final int MAX_ATTACK_TIMEOUT = 300;
   private Future<?> _aiTask;
   private final CreatureAI.SelfAnalysis _selfAnalysis = new CreatureAI.SelfAnalysis();
   private int _attackTimeout;
   private int _globalAggro;
   private boolean _thinking;
   private final int _attackRange;

   public FortSiegeGuardAI(Defender creature) {
      super(creature);
      this._selfAnalysis.init();
      this._attackTimeout = Integer.MAX_VALUE;
      this._globalAggro = -10;
      this._attackRange = this._actor.getPhysicalAttackRange();
   }

   @Override
   public void run() {
      this.notifyActionThink();
   }

   private boolean autoAttackCondition(Creature target) {
      if (target == null) {
         return false;
      } else {
         if (target instanceof Defender || target.isNpc() || target.isDoor() || target.isAlikeDead() || target instanceof FortCommander || target.isPlayable()) {
            Player player = null;
            if (target.isPlayer()) {
               player = target.asPlayer();
            } else if (target.isSummon()) {
               player = target.asSummon().getOwner();
            }

            if (player == null || player.getClan() != null && player.getClan().getFortId() == this._actor.asNpc().getFort().getResidenceId()) {
               return false;
            }
         }

         if (!target.isInvul() || (!target.isPlayer() || !target.isGM()) && (!target.isSummon() || !target.asSummon().getOwner().isGM())) {
            Creature currentTarget = target;
            if (target.isSummon()) {
               Player owner = target.asSummon().getOwner();
               if (this._actor.isInsideRadius3D(owner, 1000)) {
                  currentTarget = owner;
               }
            }

            return (!currentTarget.isPlayable() || !currentTarget.asPlayable().isSilentMovingAffected() || this._actor.isInsideRadius2D(currentTarget, 250))
               && this._actor.isAutoAttackable(currentTarget)
               && GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)currentTarget);
         } else {
            return false;
         }
      }
   }

   private void startAITask() {
      if (this._aiTask == null) {
         this._aiTask = ThreadPool.scheduleAtFixedRate(this, 1000L, 1000L);
      }
   }

   private boolean shouldPromoteIdleToActive() {
      return this._actor.isAlikeDead() ? false : World.getFirstVisibleObject(this._actor.asAttackable(), Player.class) != null;
   }

   @Override
   public synchronized void setIntentionIdle() {
      if (this.shouldPromoteIdleToActive()) {
         this.setIntentionActive();
      } else {
         super.setIntentionIdle();
         if (this._aiTask != null) {
            this._aiTask.cancel(true);
            this._aiTask = null;
         }

         this._actor.detachAI();
      }
   }

   @Override
   public synchronized void setIntentionActive() {
      super.setIntentionActive();
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionRest() {
      super.setIntentionRest();
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionAttack(WorldObject target) {
      this._attackTimeout = 300 + GameTimeTaskManager.getInstance().getGameTicks();
      super.setIntentionAttack(target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionCast(Skill skill, WorldObject target) {
      super.setIntentionCast(skill, target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionMoveTo(ILocational destination) {
      super.setIntentionMoveTo(destination);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionFollow(WorldObject target) {
      super.setIntentionFollow(target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionPickUp(WorldObject item) {
      super.setIntentionPickUp(item);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionInteract(WorldObject object) {
      super.setIntentionInteract(object);
      this.startAITask();
   }

   private void thinkActive() {
      Attackable npc = this._actor.asAttackable();
      if (this._globalAggro != 0) {
         if (this._globalAggro < 0) {
            this._globalAggro++;
         } else {
            this._globalAggro--;
         }
      }

      if (this._globalAggro >= 0) {
         World.forEachVisibleObjectInRange(npc, Creature.class, this._attackRange, target -> {
            if (this.autoAttackCondition(target) && npc.getHating(target) == 0L) {
               npc.addDamageHate(target, 0L, 1L);
            }
         });
         Creature hated = this._actor.isConfused() ? this.getAttackTarget() : npc.getMostHated();
         if (hated != null) {
            long aggro = npc.getHating(hated);
            if (aggro + this._globalAggro > 0L) {
               if (!this._actor.isRunning()) {
                  this._actor.setRunning();
               }

               this.setIntentionAttack(hated);
            }

            return;
         }
      }

      if (this._actor.getWalkSpeed() >= 0.0) {
         ((Defender)(this._actor instanceof Defender ? (Defender)this._actor : (FortCommander)this._actor)).returnHome();
      }
   }

   private void thinkAttack() {
      if (this._attackTimeout < GameTimeTaskManager.getInstance().getGameTicks() && this._actor.isRunning()) {
         this._actor.setWalking();
         this._attackTimeout = 300 + GameTimeTaskManager.getInstance().getGameTicks();
      }

      Creature attackTarget = this.getAttackTarget();
      if (attackTarget != null && !attackTarget.isAlikeDead() && this._attackTimeout >= GameTimeTaskManager.getInstance().getGameTicks()) {
         this.factionNotifyAndSupport();
         this.attackPrepare();
      } else {
         if (attackTarget != null) {
            this._actor.asAttackable().stopHating(attackTarget);
         }

         this._attackTimeout = Integer.MAX_VALUE;
         this.setAttackTarget(null);
         this.setIntentionActive();
         this._actor.setWalking();
      }
   }

   private void factionNotifyAndSupport() {
      Creature target = this.getAttackTarget();
      if (this._actor.asNpc().getTemplate().getClans() != null && target != null && !target.isInvul()) {
         for (Creature creature : World.getVisibleObjectsInRange(this._actor, Creature.class, 1000)) {
            if (!(creature instanceof Npc)) {
               if (this._selfAnalysis.hasHealOrResurrect
                  && creature.isPlayer()
                  && this._actor.asNpc().getFort().getSiege().checkIsDefender(creature.asPlayer().getClan())
                  && !this._actor.isAttackDisabled()
                  && creature.getCurrentHp() < creature.getMaxHp() * 0.6
                  && this._actor.getCurrentHp() > this._actor.getMaxHp() / 2
                  && this._actor.getCurrentMp() > this._actor.getMaxMp() / 2
                  && creature.isInCombat()) {
                  for (Skill sk : this._selfAnalysis.healSkills) {
                     if (!(this._actor.getCurrentMp() < sk.getMpConsume())
                        && !this._actor.isSkillDisabled(sk)
                        && LocationUtil.checkIfInRange(sk.getCastRange(), this._actor, creature, true)) {
                        int chance = 5;
                        if (5 < Rnd.get(100)) {
                           if (GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)creature)) {
                              WorldObject oldTarget = this._actor.getTarget();
                              this._actor.setTarget(creature);
                              this.clientStopMoving(null);
                              this._actor.doCast(sk);
                              this._actor.setTarget(oldTarget);
                              return;
                           }
                           break;
                        }
                     }
                  }
               }
            } else {
               Npc npc = creature.asNpc();
               if (npc.isInMyClan(this._actor.asNpc()) && npc.getAI() != null) {
                  if (!npc.isDead()
                     && Math.abs(target.getZ() - npc.getZ()) < 600
                     && (npc.getAI()._intention == Intention.IDLE || npc.getAI()._intention == Intention.ACTIVE)
                     && target.isInsideRadius3D(npc, 1500)
                     && GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)target)) {
                     npc.getAI().notifyActionAggression(this.getAttackTarget(), 1);
                     return;
                  }

                  if (this._selfAnalysis.hasHealOrResurrect
                     && !this._actor.isAttackDisabled()
                     && npc.getCurrentHp() < npc.getMaxHp() * 0.6
                     && this._actor.getCurrentHp() > this._actor.getMaxHp() / 2
                     && this._actor.getCurrentMp() > this._actor.getMaxMp() / 2
                     && npc.isInCombat()) {
                     for (Skill skx : this._selfAnalysis.healSkills) {
                        if (!(this._actor.getCurrentMp() < skx.getMpConsume())
                           && !this._actor.isSkillDisabled(skx)
                           && LocationUtil.checkIfInRange(skx.getCastRange(), this._actor, npc, true)) {
                           int chance = 4;
                           if (4 < Rnd.get(100)) {
                              if (GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)npc)) {
                                 WorldObject oldTarget = this._actor.getTarget();
                                 this._actor.setTarget(npc);
                                 this.clientStopMoving(null);
                                 this._actor.doCast(skx);
                                 this._actor.setTarget(oldTarget);
                                 return;
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void attackPrepare() {
      Collection<Skill> skills = null;
      double distance = 0.0;
      int range = 0;
      Creature attackTarget = this.getAttackTarget();

      try {
         this._actor.setTarget(attackTarget);
         skills = this._actor.getAllSkills();
         distance = this._actor.calculateDistance2D(attackTarget);
         range = this._actor.getPhysicalAttackRange() + this._actor.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
         if (attackTarget.isMoving()) {
            range += 50;
         }
      } catch (NullPointerException var17) {
         this._actor.setTarget(null);
         this.setIntentionIdle();
         return;
      }

      Defender sGuard = (Defender)(this._actor instanceof FortCommander ? (FortCommander)this._actor : (Defender)this._actor);
      if (attackTarget.isPlayer() && sGuard.getFort().getSiege().checkIsDefender(attackTarget.asPlayer().getClan())) {
         sGuard.stopHating(attackTarget);
         this._actor.setTarget(null);
         this.setIntentionIdle();
      } else {
         Skill sk;
         WorldObject oldTarget;
         label214: {
            Skill skx;
            WorldObject oldTargetx;
            label247: {
               if (!this._actor.isMuted() && distance > range) {
                  for (sk : skills) {
                     int castRange = sk.getCastRange();
                     if (distance <= castRange
                        && castRange > 70
                        && !this._actor.isSkillDisabled(sk)
                        && this._actor.getCurrentMp() >= this._actor.getStat().getMpConsume(sk)
                        && !sk.isPassive()) {
                        oldTarget = this._actor.getTarget();
                        if ((!sk.isContinuous() || sk.isDebuff()) && !sk.hasEffectType(EffectType.HEAL)) {
                           break label214;
                        }

                        if (!sk.hasEffectType(EffectType.HEAL) || !(this._actor.getCurrentHp() > (int)(this._actor.getMaxHp() / 1.5))) {
                           boolean useSkillSelf = true;
                           if (sk.isContinuous() && !sk.isDebuff() && this._actor.isAffectedBySkill(sk.getId())) {
                              useSkillSelf = false;
                           }

                           if (useSkillSelf) {
                              this._actor.setTarget(this._actor);
                           }
                           break label214;
                        }
                        break;
                     }
                  }

                  if (!this._actor.isAttackingNow() && this._actor.getRunSpeed() == 0.0 && this._actor.isInSurroundingRegion(attackTarget)) {
                     this._actor.setTarget(null);
                     this.setIntentionIdle();
                  } else {
                     double dx = this._actor.getX() - attackTarget.getX();
                     double dy = this._actor.getY() - attackTarget.getY();
                     double dz = this._actor.getZ() - attackTarget.getZ();
                     double homeX = attackTarget.getX() - sGuard.getSpawn().getX();
                     double homeY = attackTarget.getY() - sGuard.getSpawn().getY();
                     if (dx * dx + dy * dy > 10000.0 && homeX * homeX + homeY * homeY > 3240000.0 && this._actor.isInSurroundingRegion(attackTarget)) {
                        this._actor.setTarget(null);
                        this.setIntentionIdle();
                     } else if (dz * dz < 28900.0) {
                        if (this._selfAnalysis.isMage) {
                           range = this._selfAnalysis.maxCastRange - 50;
                        }

                        if (this._actor.getWalkSpeed() <= 0.0) {
                           return;
                        }

                        this.moveToPawn(attackTarget, attackTarget.isMoving() ? range - 70 : range);
                     }
                  }
               } else {
                  if (this._actor.isMuted() && distance > range) {
                     double dz = this._actor.getZ() - attackTarget.getZ();
                     if (dz * dz < 28900.0) {
                        if (this._selfAnalysis.isMage) {
                           range = this._selfAnalysis.maxCastRange - 50;
                        }

                        if (this._actor.getWalkSpeed() <= 0.0) {
                           return;
                        }

                        this.moveToPawn(attackTarget, attackTarget.isMoving() ? range - 70 : range);
                     }

                     return;
                  }

                  if (distance <= range) {
                     Creature hated = this._actor.isConfused() ? attackTarget : this._actor.asAttackable().getMostHated();
                     if (hated == null) {
                        this.setIntentionActive();
                        return;
                     }

                     if (hated != attackTarget) {
                        attackTarget = hated;
                     }

                     this._attackTimeout = 300 + GameTimeTaskManager.getInstance().getGameTicks();
                     if (!this._actor.isMuted() && Rnd.get(100) <= 5) {
                        for (skx : skills) {
                           int castRange = skx.getCastRange();
                           if (castRange >= distance
                              && !skx.isPassive()
                              && this._actor.getCurrentMp() >= this._actor.getStat().getMpConsume(skx)
                              && !this._actor.isSkillDisabled(skx)) {
                              oldTargetx = this._actor.getTarget();
                              if ((!skx.isContinuous() || skx.isDebuff()) && !skx.hasEffectType(EffectType.HEAL)) {
                                 break label247;
                              }

                              if (skx.hasEffectType(EffectType.HEAL) && this._actor.getCurrentHp() > (int)(this._actor.getMaxHp() / 1.5)) {
                                 break;
                              }

                              boolean useSkillSelfx = true;
                              if (skx.isContinuous() && !skx.isDebuff() && this._actor.isAffectedBySkill(skx.getId())) {
                                 useSkillSelfx = false;
                              }

                              if (useSkillSelfx) {
                                 this._actor.setTarget(this._actor);
                              }
                              break label247;
                           }
                        }
                     }

                     this._actor.doAttack(attackTarget);
                  }
               }

               return;
            }

            this.clientStopMoving(null);
            this._actor.doCast(skx);
            this._actor.setTarget(oldTargetx);
            return;
         }

         this.clientStopMoving(null);
         this._actor.doCast(sk);
         this._actor.setTarget(oldTarget);
      }
   }

   @Override
   public void notifyActionThink() {
      if (!this._thinking && !this._actor.isCastingNow() && !this._actor.isAllSkillsDisabled()) {
         this._thinking = true;

         try {
            if (this.getIntention() == Intention.ACTIVE) {
               this.thinkActive();
            } else if (this.getIntention() == Intention.ATTACK) {
               this.thinkAttack();
            }
         } finally {
            this._thinking = false;
         }
      }
   }

   @Override
   public void notifyActionAttacked(WorldObject attackerObj) {
      if (attackerObj != null) {
         Creature attacker = attackerObj.asCreature();
         if (attacker != null) {
            this._attackTimeout = 300 + GameTimeTaskManager.getInstance().getGameTicks();
            if (this._globalAggro < 0) {
               this._globalAggro = 0;
            }

            this._actor.asAttackable().addDamageHate(attacker, 0L, 1L);
            if (!this._actor.isRunning()) {
               this._actor.setRunning();
            }

            if (this.getIntention() != Intention.ATTACK) {
               this.setIntentionAttack(attacker);
            }

            super.notifyActionAttacked(attacker);
         }
      }
   }

   @Override
   public void notifyActionAggression(WorldObject targetObj, int aggro) {
      if (this._actor != null) {
         Creature target = targetObj == null ? null : targetObj.asCreature();
         Attackable me = this._actor.asAttackable();
         if (target != null) {
            me.addDamageHate(target, 0L, aggro);
            if (me.getHating(target) <= 0L) {
               if (me.getMostHated() == null) {
                  this._globalAggro = -25;
                  me.clearAggroList();
                  this.setIntentionIdle();
               }

               return;
            }

            if (this.getIntention() != Intention.ATTACK) {
               if (!this._actor.isRunning()) {
                  this._actor.setRunning();
               }

               Defender sGuard = (Defender)(this._actor instanceof FortCommander ? (FortCommander)this._actor : (Defender)this._actor);
               double homeX = target.getX() - sGuard.getSpawn().getX();
               double homeY = target.getY() - sGuard.getSpawn().getY();
               if (homeX * homeX + homeY * homeY < 3240000.0) {
                  this.setIntentionAttack(target);
               }
            }
         } else {
            if (aggro >= 0) {
               return;
            }

            Creature mostHated = me.getMostHated();
            if (mostHated == null) {
               this._globalAggro = -25;
               return;
            }

            for (Creature aggroed : me.getAggroList().keySet()) {
               me.addDamageHate(aggroed, 0L, aggro);
            }

            if (me.getHating(mostHated) <= 0L) {
               this._globalAggro = -25;
               me.clearAggroList();
               this.setIntentionIdle();
            }
         }
      }
   }

   @Override
   public void stopAITask() {
      if (this._aiTask != null) {
         this._aiTask.cancel(false);
         this._aiTask = null;
      }

      this._actor.detachAI();
      super.stopAITask();
   }
}
