package org.l2jmobius.gameserver.ai;

import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.pathfinding.PathFinding;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class SummonAI extends PlayableAI implements Runnable {
   private static final int AVOID_RADIUS = 70;
   private volatile boolean _thinking;
   private volatile boolean _startFollow = this._actor.asSummon().getFollowStatus();
   private Creature _lastAttack = null;
   private volatile boolean _startAvoid = false;
   private Future<?> _avoidTask = null;

   public SummonAI(Summon creature) {
      super(creature);
   }

   @Override
   public Intention getNextIntention() {
      return this._lastAttack != null ? Intention.ATTACK : null;
   }

   @Override
   public void setIntentionAttack(WorldObject target) {
      if (target != null) {
         Creature creatureTarget = target.asCreature();
         if (creatureTarget != null) {
            if (GeoEngineConfig.PATHFINDING <= 0
               || PathFinding.getInstance()
                     .findPath(
                        this._actor.getX(),
                        this._actor.getY(),
                        this._actor.getZ(),
                        creatureTarget.getX(),
                        creatureTarget.getY(),
                        creatureTarget.getZ(),
                        this._actor.getInstanceId(),
                        false
                     )
                  != null) {
               this.stopAvoidTask();
               super.setIntentionAttack(target);
            }
         }
      }
   }

   @Override
   public void setIntentionIdle() {
      this.stopFollow();
      this._startFollow = false;
      this.stopAvoidTask();
      this.setIntentionActive();
   }

   @Override
   public void setIntentionActive() {
      if (this._startFollow) {
         this.startAvoidTask();
         this.setIntentionFollow(this._actor.asSummon().getOwner());
      } else {
         this.startAvoidTask();
         super.setIntentionActive();
      }
   }

   @Override
   public void setIntentionFollow(WorldObject target) {
      if (target == null) {
         this.clientActionFailed();
      } else {
         Creature creatureTarget = target.asCreature();
         if (creatureTarget == null) {
            this.clientActionFailed();
         } else if (GeoEngineConfig.PATHFINDING > 0
            && PathFinding.getInstance()
                  .findPath(
                     this._actor.getX(),
                     this._actor.getY(),
                     this._actor.getZ(),
                     creatureTarget.getX(),
                     creatureTarget.getY(),
                     creatureTarget.getZ(),
                     this._actor.getInstanceId(),
                     false
                  )
               == null) {
            this.clientActionFailed();
         } else {
            this.startAvoidTask();
            super.setIntentionFollow(target);
         }
      }
   }

   private void thinkAttack() {
      if (this.checkTargetLostOrDead(this.getAttackTarget())) {
         this.setAttackTarget(null);
      } else if (!this.maybeMoveToPawn(this.getAttackTarget(), this._actor.getPhysicalAttackRange())) {
         this.clientStopMoving(null);
         this._actor.doAttack(this.getAttackTarget());
      }
   }

   private void thinkCast() {
      if (this.checkTargetLost(this.getCastTarget())) {
         this.setCastTarget(null);
      } else {
         boolean val = this._startFollow;
         if (!this.maybeMoveToPawn(this.getCastTarget(), this._actor.getMagicalAttackRange(this._skill))) {
            this.clientStopMoving(null);
            Summon summon = this._actor.asSummon();
            summon.setFollowStatus(false);
            this.setIntentionIdle();
            this._startFollow = val;
            this._actor.doCast(this._skill);
         }
      }
   }

   private void thinkPickUp() {
      if (!this.checkTargetLost(this.getTarget()) && !this.maybeMoveToPawn(this.getTarget(), 36)) {
         this.setIntentionIdle();
         this._actor.asSummon().doPickupItem(this.getTarget());
      }
   }

   private void thinkInteract() {
      if (!this.checkTargetLost(this.getTarget()) && !this.maybeMoveToPawn(this.getTarget(), 36)) {
         this.setIntentionIdle();
      }
   }

   @Override
   public void notifyActionThink() {
      if (!this._thinking && !this._actor.isCastingNow() && !this._actor.isAllSkillsDisabled()) {
         this._thinking = true;

         try {
            switch (this.getIntention()) {
               case ATTACK:
                  this.thinkAttack();
                  break;
               case CAST:
                  this.thinkCast();
                  break;
               case PICK_UP:
                  this.thinkPickUp();
                  break;
               case INTERACT:
                  this.thinkInteract();
            }
         } finally {
            this._thinking = false;
         }
      }
   }

   @Override
   public void notifyActionFinishCasting() {
      if (this._lastAttack == null) {
         this._actor.asSummon().setFollowStatus(this._startFollow);
      } else {
         Creature replayTarget = this._lastAttack;
         this._lastAttack = null;
         this.setIntentionAttack(replayTarget);
      }

      super.notifyActionFinishCasting();
   }

   @Override
   public void notifyActionAttacked(WorldObject attacker) {
      super.notifyActionAttacked(attacker);
      if (attacker != null) {
         Creature attackerCreature = attacker.asCreature();
         if (attackerCreature != null) {
            this.avoidAttack(attackerCreature);
         }
      }
   }

   @Override
   public void notifyActionEvaded(WorldObject attacker) {
      super.notifyActionEvaded(attacker);
      if (attacker != null) {
         Creature attackerCreature = attacker.asCreature();
         if (attackerCreature != null) {
            this.avoidAttack(attackerCreature);
         }
      }
   }

   private void avoidAttack(Creature attacker) {
      if (this._actor.asSummon().getOwner() != null
         && this._actor.asSummon().getOwner() != attacker
         && this._actor.asSummon().getOwner().isInsideRadius3D(this._actor, 140)) {
         this._startAvoid = true;
      }
   }

   @Override
   public void run() {
      if (this._startAvoid) {
         this._startAvoid = false;
         if (!this._actor.isMoving() && !this._actor.isDead() && !this._actor.isMovementDisabled()) {
            int ownerX = this._actor.asSummon().getOwner().getX();
            int ownerY = this._actor.asSummon().getOwner().getY();
            double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - this._actor.getY(), ownerX - this._actor.getX());
            int targetX = ownerX + (int)(70.0 * Math.cos(angle));
            int targetY = ownerY + (int)(70.0 * Math.sin(angle));
            if (GeoEngine.getInstance()
               .canMoveToTarget(this._actor.getX(), this._actor.getY(), this._actor.getZ(), targetX, targetY, this._actor.getZ(), this._actor.getInstanceId())) {
               this.moveTo(targetX, targetY, this._actor.getZ());
            }
         }
      }
   }

   public void notifyFollowStatusChange() {
      this._startFollow = !this._startFollow;
      switch (this.getIntention()) {
         case PICK_UP:
         case ACTIVE:
         case FOLLOW:
         case IDLE:
         case MOVE_TO:
            this._actor.asSummon().setFollowStatus(this._startFollow);
         case INTERACT:
      }
   }

   public void setStartFollowController(boolean value) {
      this._startFollow = value;
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
      if (this.getIntention() == Intention.ATTACK) {
         this._lastAttack = this.getAttackTarget();
      } else {
         this._lastAttack = null;
      }

      this.stopAvoidTask();
      super.setIntentionCast(skill, target);
   }

   private void startAvoidTask() {
      if (this._avoidTask == null) {
         this._avoidTask = ThreadPool.scheduleAtFixedRate(this, 100L, 100L);
      }
   }

   private void stopAvoidTask() {
      if (this._avoidTask != null) {
         this._avoidTask.cancel(false);
         this._avoidTask = null;
      }
   }

   @Override
   public void stopAITask() {
      this.stopAvoidTask();
      super.stopAITask();
   }
}
