package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStart;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public abstract class AbstractAI {
   protected final Creature _actor;
   protected Intention _intention = Intention.IDLE;
   protected volatile boolean _clientAutoAttacking;
   protected int _clientMovingToPawnOffset;
   private WorldObject _target;
   private Creature _castTarget;
   protected Creature _attackTarget;
   protected Creature _followTarget;
   protected Skill _skill;
   private int _moveToPawnTimeout;
   private NextAction _nextAction;

   public NextAction getNextAction() {
      return this._nextAction;
   }

   public void setNextAction(NextAction nextAction) {
      this._nextAction = nextAction;
   }

   protected AbstractAI(Creature creature) {
      this._actor = creature;
   }

   public Creature getActor() {
      return this._actor;
   }

   public Intention getIntention() {
      return this._intention;
   }

   public Intention getNextIntention() {
      return null;
   }

   public abstract void setIntentionIdle();

   public abstract void setIntentionActive();

   public abstract void setIntentionRest();

   public abstract void setIntentionAttack(WorldObject var1);

   public abstract void setIntentionCast(Skill var1, WorldObject var2);

   public abstract void setIntentionMoveTo(ILocational var1);

   public abstract void setIntentionFollow(WorldObject var1);

   public abstract void setIntentionPickUp(WorldObject var1);

   public abstract void setIntentionInteract(WorldObject var1);

   public abstract void notifyActionThink();

   public abstract void notifyActionAttacked(WorldObject var1);

   public abstract void notifyActionAggression(WorldObject var1, int var2);

   public abstract void notifyActionStunned();

   public abstract void notifyActionParalyzed();

   public abstract void notifyActionSleeping();

   public abstract void notifyActionRooted();

   public abstract void notifyActionConfused();

   public abstract void notifyActionMuted();

   public abstract void notifyActionEvaded(WorldObject var1);

   public abstract void notifyActionReadyToAct();

   public abstract void notifyActionUserCmd(Object var1, Object var2);

   public abstract void notifyActionArrived();

   public abstract void notifyActionArrivedRevalidate();

   public abstract void notifyActionArrivedBlocked(Location var1);

   public abstract void notifyActionForgetObject(WorldObject var1);

   public abstract void notifyActionCancel();

   public abstract void notifyActionDeath();

   public abstract void notifyActionFakeDeath();

   public abstract void notifyActionAfraid(WorldObject var1, boolean var2);

   public abstract void notifyActionFinishCasting();

   protected void clientActionFailed() {
      if (this._actor.isPlayer()) {
         this._actor.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public void moveToPawn(WorldObject pawn, int offsetValue) {
      if (!this._actor.isMovementDisabled() && !this._actor.isAttackingNow() && !this._actor.isCastingNow()) {
         int offset = offsetValue;
         if (offsetValue < 10) {
            offset = 10;
         }

         int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
         if (this._actor.isMoving() && this._target == pawn) {
            if (this._clientMovingToPawnOffset == offset) {
               if (gameTime < this._moveToPawnTimeout) {
                  return;
               }
            } else if (this._actor.isOnGeodataPath() && gameTime < this._moveToPawnTimeout + 10) {
               return;
            }
         }

         this._clientMovingToPawnOffset = offset;
         this._target = pawn;
         this._moveToPawnTimeout = gameTime;
         this._moveToPawnTimeout += 10;
         if (pawn == null) {
            return;
         }

         this._actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
         if (pawn.isCreature()) {
            if (this._actor.isOnGeodataPath()) {
               this._actor.broadcastMoveToLocation();
               this._clientMovingToPawnOffset = 0;
            } else {
               WorldRegion region = this._actor.getWorldRegion();
               if (region != null && region.isActive()) {
                  this._actor.broadcastPacket(new MoveToPawn(this._actor, pawn, offset));
               }
            }
         } else {
            this._actor.broadcastMoveToLocation();
         }
      } else {
         this.clientActionFailed();
      }
   }

   public void moveTo(ILocational loc) {
      this.moveTo(loc.getX(), loc.getY(), loc.getZ());
   }

   protected void moveTo(int x, int y, int z) {
      if (!this._actor.isMovementDisabled()) {
         this._clientMovingToPawnOffset = 0;
         this._actor.moveToLocation(x, y, z, 0);
         this._actor.broadcastMoveToLocation();
      } else {
         this.clientActionFailed();
      }
   }

   public void clientStopMoving(Location loc) {
      if (this._actor.isMoving()) {
         this._actor.stopMove(loc);
      }

      this._clientMovingToPawnOffset = 0;
   }

   protected void clientStoppedMoving() {
      if (this._clientMovingToPawnOffset > 0) {
         this._clientMovingToPawnOffset = 0;
         this._actor.broadcastPacket(new StopMove(this._actor));
      }
   }

   public boolean isAutoAttacking() {
      return this._clientAutoAttacking;
   }

   public void setAutoAttacking(boolean isAutoAttacking) {
      if (this._actor.isSummon()) {
         Summon summon = this._actor.asSummon();
         if (summon.getOwner() != null) {
            summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
         }
      } else {
         this._clientAutoAttacking = isAutoAttacking;
      }
   }

   public void clientStartAutoAttack() {
      if (!this._actor.isNpc() || this._actor.isAttackable() && !this._actor.isCoreAIDisabled()) {
         if (this._actor.isSummon()) {
            Summon summon = this._actor.asSummon();
            if (summon.getOwner() != null) {
               summon.getOwner().getAI().clientStartAutoAttack();
            }
         } else {
            if (!this._clientAutoAttacking) {
               if (this._actor.isPlayer()) {
                  Player player = this._actor.asPlayer();
                  if (player.hasSummon()) {
                     Summon summon = player.getSummon();
                     summon.broadcastPacket(new AutoAttackStart(summon.getObjectId()));
                  }
               }

               this._actor.broadcastPacket(new AutoAttackStart(this._actor.getObjectId()));
               this.setAutoAttacking(true);
            }

            AttackStanceTaskManager.getInstance().addAttackStanceTask(this._actor);
         }
      }
   }

   public void clientStopAutoAttack() {
      if (this._actor.isSummon()) {
         Summon summon = this._actor.asSummon();
         if (summon.getOwner() != null) {
            summon.getOwner().getAI().clientStopAutoAttack();
         }
      } else {
         if (this._actor.isPlayer()) {
            if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(this._actor) && this.isAutoAttacking()) {
               AttackStanceTaskManager.getInstance().addAttackStanceTask(this._actor);
            }
         } else if (this._clientAutoAttacking) {
            this._actor.broadcastPacket(new AutoAttackStop(this._actor.getObjectId()));
            this.setAutoAttacking(false);
         }
      }
   }

   public int getClientMovingToPawnOffset() {
      return this._clientMovingToPawnOffset;
   }

   protected void clientNotifyDead() {
      this._actor.broadcastPacket(new Die(this._actor));
      this._intention = Intention.IDLE;
      this._target = null;
      this._castTarget = null;
      this._attackTarget = null;
      this.stopFollow();
   }

   public void describeStateToPlayer(Player player) {
      if (this._actor.isVisibleFor(player) && this._actor.isMoving()) {
         if (this._clientMovingToPawnOffset != 0 && this.isFollowing()) {
            player.sendPacket(new MoveToPawn(this._actor, this._followTarget, this._clientMovingToPawnOffset));
         } else {
            player.sendPacket(new MoveToLocation(this._actor));
         }
      }
   }

   public boolean isFollowing() {
      return this._followTarget != null
         && this._followTarget.isCreature()
         && (this._intention == Intention.FOLLOW || CreatureFollowTaskManager.getInstance().isFollowing(this._actor));
   }

   public void startFollow(Creature target) {
      this.startFollow(target, -1);
   }

   public void startFollow(Creature target, int range) {
      this.stopFollow();
      this._followTarget = target;
      if (range == -1) {
         CreatureFollowTaskManager.getInstance().addNormalFollow(this._actor, range);
      } else {
         CreatureFollowTaskManager.getInstance().addAttackFollow(this._actor, range);
      }
   }

   public void stopFollow() {
      CreatureFollowTaskManager.getInstance().remove(this._actor);
      this._followTarget = null;
   }

   public Creature getFollowTarget() {
      return this._followTarget;
   }

   protected WorldObject getTarget() {
      return this._target;
   }

   protected void setTarget(WorldObject target) {
      this._target = target;
   }

   protected void setCastTarget(Creature target) {
      this._castTarget = target;
   }

   public Creature getCastTarget() {
      return this._castTarget;
   }

   protected void setAttackTarget(Creature target) {
      this._attackTarget = target;
   }

   public Creature getAttackTarget() {
      return this._attackTarget;
   }

   public void stopAITask() {
      this.stopFollow();
   }

   @Override
   public String toString() {
      return "Actor: " + this._actor;
   }
}
