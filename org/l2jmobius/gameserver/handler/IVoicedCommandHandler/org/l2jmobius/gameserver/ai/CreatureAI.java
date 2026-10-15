package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class CreatureAI extends AbstractAI {
   private OnNpcMoveFinished _onNpcMoveFinished = null;
   protected static final int FEAR_RANGE = 500;

   public CreatureAI(Creature creature) {
      super(creature);
   }

   private boolean canHandleAction() {
      return (this._actor.isSpawned() || this._actor.isTeleporting()) && this._actor.hasAI();
   }

   @Override
   public void notifyActionAttacked(WorldObject attacker) {
      if (this.canHandleAction()) {
         this.clientStartAutoAttack();
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.ATTACKED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void setIntentionIdle() {
      this.stopFollow();
      this._intention = Intention.IDLE;
      this.setCastTarget(null);
      this.setAttackTarget(null);
      this.clientStopMoving(null);
      this.clientStopAutoAttack();
      NextAction nextAction = this.getNextAction();
      if (nextAction != null && nextAction.isRemovedBy(Intention.IDLE)) {
         this.setNextAction(null);
      }
   }

   @Override
   public void setIntentionActive() {
      this.stopFollow();

      try {
         if (this.getIntention() == Intention.ACTIVE) {
            return;
         }

         this._intention = Intention.ACTIVE;
         WorldRegion region = this._actor.getWorldRegion();
         if (region == null || !region.areNeighborsActive()) {
            return;
         }

         this.setCastTarget(null);
         this.setAttackTarget(null);
         this.clientStopMoving(null);
         this.clientStopAutoAttack();
         this.notifyActionThink();
      } finally {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isRemovedBy(Intention.ACTIVE)) {
            this.setNextAction(null);
         }
      }
   }

   @Override
   public void setIntentionRest() {
      this.stopFollow();
      this.setIntentionIdle();
      NextAction nextAction = this.getNextAction();
      if (nextAction != null && nextAction.isRemovedBy(Intention.REST)) {
         this.setNextAction(null);
      }
   }

   @Override
   public void setIntentionAttack(WorldObject target) {
      if (target == null) {
         this.clientActionFailed();
      } else {
         Creature attackTarget = target.asCreature();

         try {
            if (attackTarget == null
               || this.getIntention() == Intention.REST
               || this._actor.isAllSkillsDisabled()
               || this._actor.isCastingNow()
               || this._actor.isControlBlocked()) {
               this.clientActionFailed();
               return;
            }

            if (this.getIntention() == Intention.ATTACK) {
               if (this.getAttackTarget() != attackTarget) {
                  this.setAttackTarget(attackTarget);
                  this.stopFollow();
                  this.notifyActionThink();
               } else {
                  this.stopFollow();
                  this.notifyActionThink();
               }
            } else {
               this._intention = Intention.ATTACK;
               this.setAttackTarget(attackTarget);
               this.stopFollow();
               this.notifyActionThink();
            }
         } finally {
            NextAction nextAction = this.getNextAction();
            if (nextAction != null && nextAction.isRemovedBy(Intention.ATTACK)) {
               this.setNextAction(null);
            }
         }
      }
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
      this.stopFollow();

      try {
         if (this.getIntention() != Intention.REST || !skill.isMagic()) {
            int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
            int bowAttackEndTime = this._actor.getBowAttackEndTime();
            if (bowAttackEndTime > gameTime) {
               ThreadPool.schedule(() -> {
                  if (this._actor.isAttackingNow()) {
                     this._actor.abortAttack();
                  }

                  this.changeIntentionToCast(skill, target);
               }, (bowAttackEndTime - gameTime) * 100);
            } else {
               this.changeIntentionToCast(skill, target);
            }

            return;
         }

         this.clientActionFailed();
         this._actor.setCastingNow(false);
      } finally {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isRemovedBy(Intention.CAST)) {
            this.setNextAction(null);
         }
      }
   }

   protected void changeIntentionToCast(Skill skill, WorldObject target) {
      this.setCastTarget(target == null ? null : target.asCreature());
      this._skill = skill;
      this._intention = Intention.CAST;
      this.notifyActionThink();
   }

   @Override
   public void setIntentionMoveTo(ILocational loc) {
      this.stopFollow();

      try {
         if (this.getIntention() == Intention.REST || this._actor.isAllSkillsDisabled() || this._actor.isCastingNow()) {
            this.clientActionFailed();
            return;
         }

         this._intention = Intention.MOVE_TO;
         this.clientStopAutoAttack();
         this._actor.abortAttack();
         this.moveTo(loc.getX(), loc.getY(), loc.getZ());
      } finally {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isRemovedBy(Intention.MOVE_TO)) {
            this.setNextAction(null);
         }
      }
   }

   @Override
   public void setIntentionFollow(WorldObject target) {
      if (target == null) {
         this.clientActionFailed();
      } else {
         Creature followTarget = target.asCreature();

         try {
            if (followTarget == null
               || this.getIntention() == Intention.REST
               || this._actor.isAllSkillsDisabled()
               || this._actor.isCastingNow()
               || this._actor.isMovementDisabled()
               || this._actor.isDead()
               || this._actor == followTarget) {
               this.clientActionFailed();
               return;
            }

            this.clientStopAutoAttack();
            this._intention = Intention.FOLLOW;
            this.startFollow(followTarget);
         } finally {
            NextAction nextAction = this.getNextAction();
            if (nextAction != null && nextAction.isRemovedBy(Intention.FOLLOW)) {
               this.setNextAction(null);
            }
         }
      }
   }

   @Override
   public void setIntentionPickUp(WorldObject object) {
      this.stopFollow();

      try {
         if (object == null || this.getIntention() == Intention.REST || this._actor.isAllSkillsDisabled() || this._actor.isCastingNow()) {
            this.clientActionFailed();
            return;
         }

         this.clientStopAutoAttack();
         if (!object.isItem() || ((Item)object).getItemLocation() == ItemLocation.VOID) {
            this._intention = Intention.PICK_UP;
            this.setTarget(object);
            if (object.getX() == 0 && object.getY() == 0) {
               object.setXYZ(this.getActor().getX(), this.getActor().getY(), this.getActor().getZ() + 5);
            }

            this.moveToPawn(object, 20);
            return;
         }
      } finally {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isRemovedBy(Intention.PICK_UP)) {
            this.setNextAction(null);
         }
      }
   }

   @Override
   public void setIntentionInteract(WorldObject object) {
      this.stopFollow();

      try {
         if (object == null || this.getIntention() == Intention.REST || this._actor.isAllSkillsDisabled() || this._actor.isCastingNow()) {
            this.clientActionFailed();
            return;
         }

         this.clientStopAutoAttack();
         if (this.getIntention() != Intention.INTERACT) {
            this._intention = Intention.INTERACT;
            this.setTarget(object);
            this.moveToPawn(object, 60);
            return;
         }
      } finally {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isRemovedBy(Intention.INTERACT)) {
            this.setNextAction(null);
         }
      }
   }

   @Override
   public void notifyActionThink() {
   }

   @Override
   public void notifyActionAggression(WorldObject target, int aggro) {
   }

   @Override
   public void notifyActionStunned() {
      if (this.canHandleAction()) {
         this._actor.broadcastPacket(new AutoAttackStop(this._actor.getObjectId()));
         if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._actor)) {
            AttackStanceTaskManager.getInstance().removeAttackStanceTask(this._actor);
         }

         this.setAutoAttacking(false);
         this.clientStopMoving(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.STUNNED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionParalyzed() {
      if (this.canHandleAction()) {
         this._actor.broadcastPacket(new AutoAttackStop(this._actor.getObjectId()));
         if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._actor)) {
            AttackStanceTaskManager.getInstance().removeAttackStanceTask(this._actor);
         }

         this.setAutoAttacking(false);
         this.clientStopMoving(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.PARALYZED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionSleeping() {
      if (this.canHandleAction()) {
         this._actor.broadcastPacket(new AutoAttackStop(this._actor.getObjectId()));
         if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._actor)) {
            AttackStanceTaskManager.getInstance().removeAttackStanceTask(this._actor);
         }

         this.setAutoAttacking(false);
         this.clientStopMoving(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.SLEEPING)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionRooted() {
      if (this.canHandleAction()) {
         this.clientStopMoving(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.ROOTED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionConfused() {
      if (this.canHandleAction()) {
         this.clientStopMoving(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.CONFUSED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionMuted() {
      if (this.canHandleAction()) {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.MUTED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionEvaded(WorldObject attacker) {
   }

   @Override
   public void notifyActionReadyToAct() {
      if (this.canHandleAction()) {
         if (!this._actor.isCastingNow() && !this._actor.isCastingSimultaneouslyNow()) {
            this.notifyActionThink();
         }

         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.READY_TO_ACT)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionUserCmd(Object arg0, Object arg1) {
   }

   @Override
   public void notifyActionArrived() {
      if (this.canHandleAction()) {
         try {
            if (this._actor.isCastingNow() || this._actor.isCastingSimultaneouslyNow()) {
               return;
            }

            this._actor.revalidateZone(true);
            if (!this._actor.moveToNextRoutePoint()) {
               this.clientStoppedMoving();
               if (this._actor.isNpc()) {
                  Npc npc = this._actor.asNpc();
                  WalkingManager.getInstance().onArrived(npc);
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_FINISHED, npc)) {
                     if (this._onNpcMoveFinished == null) {
                        this._onNpcMoveFinished = new OnNpcMoveFinished(npc);
                     }

                     EventDispatcher.getInstance().notifyEventAsync(this._onNpcMoveFinished, npc);
                  }
               }

               if (this.getIntention() == Intention.MOVE_TO) {
                  this.setIntentionActive();
               }

               this.notifyActionThink();
               return;
            }
         } finally {
            NextAction nextAction = this.getNextAction();
            if (nextAction != null && nextAction.isTriggeredBy(Action.ARRIVED)) {
               this.setNextAction(null);
               nextAction.doAction();
            }
         }
      }
   }

   @Override
   public void notifyActionArrivedRevalidate() {
      if (this.canHandleAction()) {
         if (this._actor.isMoving()) {
            this.notifyActionThink();
         }

         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.ARRIVED_REVALIDATE)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionArrivedBlocked(Location location) {
      if (this.canHandleAction()) {
         if (this.getIntention() == Intention.MOVE_TO || this.getIntention() == Intention.CAST) {
            this.setIntentionActive();
         }

         this.clientStopMoving(location);
         this.notifyActionThink();
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.ARRIVED_BLOCKED)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionForgetObject(WorldObject object) {
      if (this.canHandleAction()) {
         try {
            this._actor.removeSeenCreature(object);
            if (this.getTarget() == object) {
               this.setTarget(null);
               if (this.getIntention() == Intention.INTERACT || this.getIntention() == Intention.PICK_UP) {
                  this.setIntentionActive();
               }
            }

            if (this.getAttackTarget() == object) {
               this.setAttackTarget(null);
               if (object == null || !object.isCreature() || !object.asCreature().isAlikeDead()) {
                  this.setIntentionActive();
               }
            }

            if (this.getCastTarget() == object) {
               this.setCastTarget(null);
               if (object == null || !object.isCreature() || !object.asCreature().isAlikeDead()) {
                  this.setIntentionActive();
               }
            }

            if (this.getFollowTarget() == object) {
               this.clientStopMoving(null);
               this.stopFollow();
               this.setIntentionActive();
            }

            if (this._actor == object) {
               this.setTarget(null);
               this.setAttackTarget(null);
               this.setCastTarget(null);
               this.stopFollow();
               this.clientStopMoving(null);
               this._intention = Intention.IDLE;
               return;
            }
         } finally {
            NextAction nextAction = this.getNextAction();
            if (nextAction != null && nextAction.isTriggeredBy(Action.FORGET_OBJECT)) {
               nextAction.doAction();
            }
         }
      }
   }

   @Override
   public void notifyActionCancel() {
      if (this.canHandleAction()) {
         this._actor.abortCast();
         this.stopFollow();
         if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._actor)) {
            this._actor.broadcastPacket(new AutoAttackStop(this._actor.getObjectId()));
         }

         this.notifyActionThink();
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.CANCEL)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionDeath() {
      if (this.canHandleAction()) {
         this.stopAITask();
         if (this._actor.isNpc()) {
            this._actor.asNpc().setDisplayEffect(0);
         }

         this.clientNotifyDead();
         if (!this._actor.isPlayable() && !this._actor.isFakePlayer()) {
            this._actor.setWalking();
         }

         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.DEATH)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionFakeDeath() {
      if (this.canHandleAction()) {
         this.stopFollow();
         this.clientStopMoving(null);
         this._intention = Intention.IDLE;
         this.setTarget(null);
         this.setCastTarget(null);
         this.setAttackTarget(null);
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.FAKE_DEATH)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionFinishCasting() {
      if (this.canHandleAction()) {
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.FINISH_CASTING)) {
            nextAction.doAction();
         }
      }
   }

   @Override
   public void notifyActionAfraid(WorldObject effector, boolean start) {
      if (this.canHandleAction() && effector != null) {
         Creature effectorCreature = effector.asCreature();
         double radians = Math.toRadians(
            start && effectorCreature != null
               ? LocationUtil.calculateAngleFrom(effectorCreature, this._actor)
               : LocationUtil.convertHeadingToDegree(this._actor.getHeading())
         );
         int posX = (int)(this._actor.getX() + 500.0 * Math.cos(radians));
         int posY = (int)(this._actor.getY() + 500.0 * Math.sin(radians));
         int posZ = this._actor.getZ();
         if (!this._actor.isPet()) {
            this._actor.setRunning();
         }

         this.setIntentionMoveTo(
            GeoEngineConfig.PATHFINDING > 0
               ? GeoEngine.getInstance()
                  .getValidLocation(this._actor.getX(), this._actor.getY(), this._actor.getZ(), posX, posY, posZ, this._actor.getInstanceId())
               : new Location(posX, posY, posZ)
         );
         NextAction nextAction = this.getNextAction();
         if (nextAction != null && nextAction.isTriggeredBy(Action.AFRAID)) {
            nextAction.doAction();
         }
      }
   }

   protected boolean maybeMoveToPosition(ILocational worldPosition, int offset) {
      if (worldPosition == null) {
         return false;
      } else if (offset < 0) {
         return false;
      } else if (!this._actor.isInsideRadius2D(worldPosition, offset + this._actor.getTemplate().getCollisionRadius())) {
         if (this._actor.isMovementDisabled()) {
            return true;
         } else {
            if (!this._actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI)) {
               this._actor.setRunning();
            }

            this.stopFollow();
            int x = this._actor.getX();
            int y = this._actor.getY();
            double dx = worldPosition.getX() - x;
            double dy = worldPosition.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double sin = dy / dist;
            double cos = dx / dist;
            dist -= offset - 5;
            x += (int)(dist * cos);
            y += (int)(dist * sin);
            this.moveTo(x, y, worldPosition.getZ());
            return true;
         }
      } else {
         if (this.isFollowing()) {
            this.stopFollow();
         }

         return false;
      }
   }

   protected boolean maybeMoveToPawn(WorldObject target, int offsetValue) {
      if (target == null) {
         return false;
      } else if (offsetValue < 0) {
         return false;
      } else {
         int offsetWithCollision = offsetValue + this._actor.getTemplate().getCollisionRadius();
         if (target.isCreature()) {
            offsetWithCollision += target.asCreature().getTemplate().getCollisionRadius();
         }

         if (!this._actor.isInsideRadius2D(target, offsetWithCollision)) {
            if (this.isFollowing()) {
               if (!this._actor.isInsideRadius2D(target, offsetWithCollision + 100)) {
                  return true;
               } else {
                  this.stopFollow();
                  return false;
               }
            } else if (!this._actor.isMovementDisabled() && !(this._actor.getMoveSpeed() <= 0.0)) {
               if (this._actor.getAI().getIntention() == Intention.CAST
                  && this._actor.isPlayer()
                  && this._actor.isTransformed()
                  && !this._actor.getTransformation().isCombat()) {
                  this._actor.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
                  this._actor.sendPacket(ActionFailed.STATIC_PACKET);
                  return true;
               } else {
                  if (!this._actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI)) {
                     this._actor.setRunning();
                  }

                  this.stopFollow();
                  int offset = offsetValue;
                  if (target.isCreature() && !target.isDoor()) {
                     if (target.asCreature().isMoving()) {
                        offset = offsetValue - 100;
                     }

                     if (offset < 5) {
                        offset = 5;
                     }

                     this.startFollow(target.asCreature(), offset);
                  } else {
                     this.moveToPawn(target, offsetValue);
                  }

                  return true;
               }
            } else {
               if (this._actor.getAI().getIntention() == Intention.ATTACK) {
                  this._actor.getAI().setIntentionIdle();
               }

               return true;
            }
         } else {
            if (this.isFollowing()) {
               this.stopFollow();
            }

            return false;
         }
      }
   }

   protected boolean checkTargetLostOrDead(Creature target) {
      if (target != null && !target.isDead()) {
         return false;
      } else {
         this.setIntentionActive();
         return true;
      }
   }

   protected boolean checkTargetLost(WorldObject target) {
      if (target == null) {
         this.setIntentionActive();
         return true;
      } else {
         if (this._actor != null) {
            if (this._skill != null && this._skill.hasNegativeEffect() && this._skill.getAffectRange() > 0) {
               if (this._actor.isPlayer() && this._actor.isMoving()) {
                  if (!GeoEngine.getInstance().canMoveToTarget(this._actor, target)) {
                     this.setIntentionActive();
                     return true;
                  }
               } else if (!GeoEngine.getInstance().canSeeTarget(this._actor, target)) {
                  this.setIntentionActive();
                  return true;
               }
            }

            if (this._actor.isSummon()) {
               if (GeoEngine.getInstance().canMoveToTarget(this._actor, target)) {
                  return false;
               }

               this.setIntentionActive();
               return true;
            }
         }

         return false;
      }
   }

   public boolean canAura(Skill sk) {
      return (
            sk.getTargetType() == TargetType.AURA
               || sk.getTargetType() == TargetType.BEHIND_AURA
               || sk.getTargetType() == TargetType.FRONT_AURA
               || sk.getTargetType() == TargetType.AURA_CORPSE_MOB
         )
         && World.getFirstVisibleObjectInRange(this._actor, Creature.class, sk.getAffectRange(), target -> target == this.getAttackTarget()) != null;
   }

   public boolean canAOE(Skill sk) {
      if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)) {
         if (sk.getTargetType() == TargetType.AURA
            || sk.getTargetType() == TargetType.BEHIND_AURA
            || sk.getTargetType() == TargetType.FRONT_AURA
            || sk.getTargetType() == TargetType.AURA_CORPSE_MOB) {
            boolean cancast = World.getFirstVisibleObjectInRange(
                  this._actor,
                  Creature.class,
                  sk.getAffectRange(),
                  targetx -> GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)targetx)
                     && (!targetx.isAttackable() || this._actor.asNpc().isChaos())
                     && targetx.isAffectedBySkill(sk.getId())
               )
               == null;
            if (cancast) {
               return true;
            }
         } else if (sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA) {
            boolean cancast = true;

            for (Creature target : World.getVisibleObjectsInRange(this.getAttackTarget(), Creature.class, sk.getAffectRange())) {
               if (GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)target)
                  && target != null
                  && (!target.isAttackable() || this._actor.asNpc().isChaos())
                  && !target.getEffectList().isEmpty()) {
                  cancast = true;
               }
            }

            if (cancast) {
               return true;
            }
         }
      } else if (sk.getTargetType() == TargetType.AURA
         || sk.getTargetType() == TargetType.BEHIND_AURA
         || sk.getTargetType() == TargetType.FRONT_AURA
         || sk.getTargetType() == TargetType.AURA_CORPSE_MOB) {
         boolean cancast = World.getFirstVisibleObjectInRange(
               this._actor,
               Creature.class,
               sk.getAffectRange(),
               targetx -> GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)targetx)
                  && (!targetx.isAttackable() || this._actor.asNpc().isChaos())
                  && !targetx.getEffectList().isEmpty()
            )
            != null;
         if (cancast) {
            return true;
         }
      } else if (sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA) {
         boolean cancast = World.getFirstVisibleObjectInRange(
               this.getAttackTarget(),
               Creature.class,
               sk.getAffectRange(),
               targetx -> GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)targetx)
                  && (!targetx.isAttackable() || this._actor.asNpc().isChaos())
                  && targetx.isAffectedBySkill(sk.getId())
            )
            == null;
         if (cancast) {
            return true;
         }
      }

      return false;
   }

   public boolean canParty(Skill sk) {
      if (this.isParty(sk)) {
         int count = 0;
         int ccount = 0;

         for (Attackable target : World.getVisibleObjectsInRange(this._actor, Attackable.class, sk.getAffectRange())) {
            if (GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)target) && target.isInMyClan(this._actor.asNpc())) {
               count++;
               if (target.isAffectedBySkill(sk.getId())) {
                  ccount++;
               }
            }
         }

         if (ccount < count) {
            return true;
         }
      }

      return false;
   }

   public boolean isParty(Skill sk) {
      return sk.getTargetType() == TargetType.PARTY;
   }

   protected class SelfAnalysis {
      public boolean isMage;
      public boolean isBalanced;
      public boolean isArcher;
      public boolean isHealer;
      public boolean isFighter;
      public boolean cannotMoveOnLand;
      public List<Skill> generalSkills;
      public List<Skill> buffSkills;
      public int lastBuffTick;
      public List<Skill> debuffSkills;
      public int lastDebuffTick;
      public List<Skill> cancelSkills;
      public List<Skill> healSkills;
      public List<Skill> generalDisablers;
      public List<Skill> sleepSkills;
      public List<Skill> rootSkills;
      public List<Skill> muteSkills;
      public List<Skill> resurrectSkills;
      public boolean hasHealOrResurrect;
      public boolean hasLongRangeSkills;
      public boolean hasLongRangeDamageSkills;
      public int maxCastRange;

      public SelfAnalysis() {
         Objects.requireNonNull(CreatureAI.this);
         super();
         this.isMage = false;
         this.isArcher = false;
         this.isHealer = false;
         this.isFighter = false;
         this.cannotMoveOnLand = false;
         this.generalSkills = new ArrayList<>();
         this.buffSkills = new ArrayList<>();
         this.lastBuffTick = 0;
         this.debuffSkills = new ArrayList<>();
         this.lastDebuffTick = 0;
         this.cancelSkills = new ArrayList<>();
         this.healSkills = new ArrayList<>();
         this.generalDisablers = new ArrayList<>();
         this.sleepSkills = new ArrayList<>();
         this.rootSkills = new ArrayList<>();
         this.muteSkills = new ArrayList<>();
         this.resurrectSkills = new ArrayList<>();
         this.hasHealOrResurrect = false;
         this.hasLongRangeSkills = false;
         this.hasLongRangeDamageSkills = false;
         this.maxCastRange = 0;
      }

      public void init() {
         switch (((NpcTemplate)CreatureAI.this._actor.getTemplate()).getAIType()) {
            case FIGHTER:
               this.isFighter = true;
               break;
            case MAGE:
               this.isMage = true;
               break;
            case CORPSE:
            case BALANCED:
               this.isBalanced = true;
               break;
            case ARCHER:
               this.isArcher = true;
               break;
            case HEALER:
               this.isHealer = true;
               break;
            default:
               this.isFighter = true;
         }

         if (CreatureAI.this._actor.isNpc()) {
            switch (CreatureAI.this._actor.getId()) {
               case 20314:
               case 20849:
                  this.cannotMoveOnLand = true;
                  break;
               default:
                  this.cannotMoveOnLand = false;
            }
         }

         for (Skill sk : CreatureAI.this._actor.getAllSkills()) {
            if (!sk.isPassive()) {
               int castRange = sk.getCastRange();
               boolean hasLongRangeDamageSkill = false;
               if (sk.isContinuous()) {
                  if (!sk.isDebuff()) {
                     this.buffSkills.add(sk);
                  } else {
                     this.debuffSkills.add(sk);
                  }
               } else {
                  if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)) {
                     this.cancelSkills.add(sk);
                  } else if (sk.hasEffectType(EffectType.HEAL)) {
                     this.healSkills.add(sk);
                     this.hasHealOrResurrect = true;
                  } else if (sk.hasEffectType(EffectType.SLEEP)) {
                     this.sleepSkills.add(sk);
                  } else if (sk.hasEffectType(EffectType.STUN, EffectType.PARALYZE)) {
                     switch (sk.getId()) {
                        case 367:
                        case 4111:
                        case 4383:
                        case 4578:
                        case 4616:
                           this.sleepSkills.add(sk);
                           break;
                        default:
                           this.generalDisablers.add(sk);
                     }
                  } else if (sk.hasEffectType(EffectType.ROOT)) {
                     this.rootSkills.add(sk);
                  } else if (sk.hasEffectType(EffectType.FEAR)) {
                     this.debuffSkills.add(sk);
                  } else if (sk.hasEffectType(EffectType.MUTE)) {
                     this.muteSkills.add(sk);
                  } else if (sk.hasEffectType(EffectType.RESURRECTION)) {
                     this.resurrectSkills.add(sk);
                     this.hasHealOrResurrect = true;
                  } else {
                     this.generalSkills.add(sk);
                     hasLongRangeDamageSkill = true;
                  }

                  if (castRange > 150) {
                     this.hasLongRangeSkills = true;
                     if (hasLongRangeDamageSkill) {
                        this.hasLongRangeDamageSkills = true;
                     }
                  }

                  if (castRange > this.maxCastRange) {
                     this.maxCastRange = castRange;
                  }
               }
            }
         }

         if (!this.hasLongRangeDamageSkills && this.isMage) {
            this.isBalanced = true;
            this.isMage = false;
            this.isFighter = false;
         }

         if (!this.hasLongRangeSkills && (this.isMage || this.isBalanced)) {
            this.isBalanced = false;
            this.isMage = false;
            this.isFighter = true;
         }

         if (this.generalSkills.isEmpty() && this.isMage) {
            this.isBalanced = true;
            this.isMage = false;
         }
      }
   }

   protected class TargetAnalysis {
      public Creature creature;
      public boolean isMage;
      public boolean isBalanced;
      public boolean isArcher;
      public boolean isFighter;
      public boolean isCanceled;
      public boolean isSlower;
      public boolean isMagicResistant;

      public TargetAnalysis() {
         Objects.requireNonNull(CreatureAI.this);
         super();
      }

      public void update(Creature target) {
         if (target != this.creature || Rnd.get(100) <= 25) {
            this.creature = target;
            if (target != null) {
               this.isMage = false;
               this.isBalanced = false;
               this.isArcher = false;
               this.isFighter = false;
               this.isCanceled = false;
               if (target.getMAtk(null, null) > 1.5 * target.getPAtk(null)) {
                  this.isMage = true;
               } else if (!(target.getPAtk(null) * 0.8 < target.getMAtk(null, null)) && !(target.getMAtk(null, null) * 0.8 > target.getPAtk(null))) {
                  Weapon weapon = target.getActiveWeaponItem();
                  if (weapon == null || weapon.getItemType() != WeaponType.BOW && weapon.getItemType() != WeaponType.CROSSBOW) {
                     this.isFighter = true;
                  } else {
                     this.isArcher = true;
                  }
               } else {
                  this.isBalanced = true;
               }

               this.isSlower = target.getRunSpeed() < CreatureAI.this._actor.getRunSpeed() - 3.0;
               this.isMagicResistant = target.getMDef(null, null) * 1.2 > CreatureAI.this._actor.getMAtk(null, null);
               if (target.getBuffCount() < 4) {
                  this.isCanceled = true;
               }
            }
         }
      }
   }
}
