package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PlayerAI extends PlayableAI {
   private boolean _thinking;
   private Intention _savedIntention = null;
   private WorldObject _savedAttackTarget = null;
   private ILocational _savedMoveTo = null;
   private WorldObject _savedFollowTarget = null;
   private WorldObject _savedPickUpTarget = null;
   private WorldObject _savedInteractTarget = null;
   private Skill _savedCastSkill = null;
   private WorldObject _savedCastTarget = null;

   public PlayerAI(Player player) {
      super(player);
   }

   @Override
   public Intention getNextIntention() {
      return this._savedIntention;
   }

   private void clearSavedIntention() {
      this._savedIntention = null;
      this._savedAttackTarget = null;
      this._savedMoveTo = null;
      this._savedFollowTarget = null;
      this._savedPickUpTarget = null;
      this._savedInteractTarget = null;
      this._savedCastSkill = null;
      this._savedCastTarget = null;
   }

   private void saveCurrentIntentionForCast() {
      this._savedIntention = this._intention;
      this._savedAttackTarget = this.getAttackTarget();
      this._savedCastSkill = this._skill;
      this._savedCastTarget = this.getCastTarget();
      this._savedMoveTo = null;
      this._savedFollowTarget = this.getFollowTarget();
      this._savedPickUpTarget = null;
      this._savedInteractTarget = null;
   }

   private void replaySavedIntention() {
      if (this._savedIntention != null) {
         Intention intention = this._savedIntention;
         WorldObject attackTarget = this._savedAttackTarget;
         ILocational moveToLoc = this._savedMoveTo;
         WorldObject followTarget = this._savedFollowTarget;
         WorldObject pickUpTarget = this._savedPickUpTarget;
         WorldObject interactTarget = this._savedInteractTarget;
         Skill castSkill = this._savedCastSkill;
         WorldObject castTarget = this._savedCastTarget;
         this.clearSavedIntention();
         switch (intention) {
            case IDLE:
               this.setIntentionIdle();
               break;
            case ACTIVE:
               this.setIntentionActive();
               break;
            case REST:
               this.setIntentionRest();
               break;
            case ATTACK:
               this.setIntentionAttack(attackTarget);
               break;
            case CAST:
               if (castSkill != null) {
                  this.setIntentionCast(castSkill, castTarget);
               }
               break;
            case MOVE_TO:
               if (moveToLoc != null) {
                  this.setIntentionMoveTo(moveToLoc);
               }
               break;
            case FOLLOW:
               this.setIntentionFollow(followTarget);
               break;
            case PICK_UP:
               this.setIntentionPickUp(pickUpTarget);
               break;
            case INTERACT:
               this.setIntentionInteract(interactTarget);
         }
      }
   }

   @Override
   public void notifyActionReadyToAct() {
      if (this._savedIntention != null) {
         this.replaySavedIntention();
      }

      super.notifyActionReadyToAct();
   }

   @Override
   public void notifyActionForgetObject(WorldObject object) {
      if (object != null && object.isPlayer()) {
         this.getActor().getKnownRelations().remove(object.getObjectId());
      }

      super.notifyActionForgetObject(object);
   }

   @Override
   public void notifyActionCancel() {
      this.clearSavedIntention();
      super.notifyActionCancel();
   }

   @Override
   public void notifyActionFinishCasting() {
      if (this.getIntention() == Intention.CAST) {
         if (this._savedIntention != null) {
            if (this._savedIntention != Intention.CAST) {
               this.replaySavedIntention();
            } else {
               this.clearSavedIntention();
               this.setIntentionIdle();
            }
         } else {
            this.setIntentionIdle();
         }
      }

      super.notifyActionFinishCasting();
   }

   @Override
   public void setIntentionRest() {
      if (this.getIntention() != Intention.REST) {
         this.clearSavedIntention();
         this._intention = Intention.REST;
         this.setTarget(null);
         if (this.getAttackTarget() != null) {
            this.setAttackTarget(null);
         }

         this.clientStopMoving(null);
      }
   }

   @Override
   public void setIntentionActive() {
      this.setIntentionIdle();
   }

   @Override
   public void setIntentionMoveTo(ILocational loc) {
      if (this.getIntention() == Intention.REST) {
         this.clientActionFailed();
      } else {
         Player player = this._actor.asPlayer();
         if (player.getDuelState() == 2) {
            this.clientActionFailed();
            player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOVE_WHILE_FROZEN_PLEASE_WAIT));
         } else if (!this._actor.isAllSkillsDisabled() && !this._actor.isCastingNow() && !this._actor.isAttackingNow()) {
            this.stopFollow();
            this.clearSavedIntention();
            this._intention = Intention.MOVE_TO;
            this.clientStopAutoAttack();
            this._actor.abortAttack();
            this.moveTo(loc.getX(), loc.getY(), loc.getZ());
         } else {
            this.clientActionFailed();
            this._savedIntention = Intention.MOVE_TO;
            this._savedMoveTo = loc;
         }
      }
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
      if ((skill == null || !skill.isToggle()) && this._intention != Intention.CAST) {
         this.saveCurrentIntentionForCast();
      }

      super.setIntentionCast(skill, target);
   }

   @Override
   protected void clientNotifyDead() {
      this._clientMovingToPawnOffset = 0;
      super.clientNotifyDead();
   }

   private void thinkAttack() {
      Creature target = this.getAttackTarget();
      if (target != null) {
         if (this.checkTargetLostOrDead(target)) {
            this.setAttackTarget(null);
         } else if (!this.maybeMoveToPawn(target, this._actor.getPhysicalAttackRange())) {
            this.clientStopMoving(null);
            this._actor.doAttack(target);
         }
      }
   }

   private void thinkCast() {
      Creature target = this.getCastTarget();
      if (this._skill.getTargetType() == TargetType.GROUND && this._actor.isPlayer()) {
         if (this.maybeMoveToPosition(this._actor.asPlayer().getCurrentSkillWorldPosition(), this._actor.getMagicalAttackRange(this._skill))) {
            this._actor.setCastingNow(false);
            return;
         }
      } else {
         if (this.checkTargetLost(target)) {
            if (this._skill.hasNegativeEffect() && this.getAttackTarget() != null) {
               this.setCastTarget(null);
            }

            this._actor.setCastingNow(false);
            return;
         }

         if (target != null && this.maybeMoveToPawn(target, this._actor.getMagicalAttackRange(this._skill))) {
            this._actor.setCastingNow(false);
            return;
         }
      }

      if (this._skill.getHitTime() > 50 && !this._skill.isSimultaneousCast()) {
         this.clientStopMoving(null);
      }

      WorldObject currentTarget = this._actor.getTarget();
      if (currentTarget != target && currentTarget != null && target != null) {
         this._actor.setTarget(target);
         this._actor.doCast(this._skill);
         this._actor.setTarget(currentTarget);
      } else {
         this._actor.doCast(this._skill);
      }
   }

   private void thinkPickUp() {
      if (!this._actor.isAllSkillsDisabled() && !this._actor.isCastingNow()) {
         WorldObject target = this.getTarget();
         if (!this.checkTargetLost(target) && !this.maybeMoveToPawn(target, 36)) {
            this.setIntentionIdle();
            this._actor.asPlayer().doPickupItem(target);
         }
      }
   }

   private void thinkInteract() {
      if (!this._actor.isAllSkillsDisabled() && !this._actor.isCastingNow()) {
         WorldObject target = this.getTarget();
         if (!this.checkTargetLost(target) && !this.maybeMoveToPawn(target, 36)) {
            if (!(target instanceof StaticObject)) {
               this._actor.asPlayer().doInteract(target.asCreature());
            }

            this.setIntentionIdle();
         }
      }
   }

   @Override
   public void notifyActionThink() {
      if (!this._thinking || this.getIntention() == Intention.CAST) {
         this._thinking = true;

         try {
            if (this.getIntention() == Intention.ATTACK) {
               this.thinkAttack();
            } else if (this.getIntention() == Intention.CAST) {
               this.thinkCast();
            } else if (this.getIntention() == Intention.PICK_UP) {
               this.thinkPickUp();
            } else if (this.getIntention() == Intention.INTERACT) {
               this.thinkInteract();
            }
         } finally {
            this._thinking = false;
         }
      }
   }
}
