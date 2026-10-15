package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExStopMoveAirShip;

public class AirShipAI extends CreatureAI {
   public AirShipAI(AirShip airShip) {
      super(airShip);
   }

   @Override
   protected void moveTo(int x, int y, int z) {
      if (!this._actor.isMovementDisabled()) {
         this._actor.moveToLocation(x, y, z, 0);
         this._actor.broadcastPacket(new ExMoveToLocationAirShip(this.getActor()));
      }
   }

   @Override
   public void clientStopMoving(Location loc) {
      if (this._actor.isMoving()) {
         this._actor.stopMove(loc);
         this._actor.broadcastPacket(new ExStopMoveAirShip(this.getActor()));
      } else {
         if (loc != null) {
            this._actor.broadcastPacket(new ExStopMoveAirShip(this.getActor()));
         }
      }
   }

   @Override
   public void describeStateToPlayer(Player player) {
      if (this._actor.isMoving()) {
         player.sendPacket(new ExMoveToLocationAirShip(this.getActor()));
      }
   }

   @Override
   public void setIntentionAttack(WorldObject target) {
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
   }

   @Override
   public void setIntentionFollow(WorldObject target) {
   }

   @Override
   public void setIntentionPickUp(WorldObject item) {
   }

   @Override
   public void setIntentionInteract(WorldObject object) {
   }

   @Override
   public void notifyActionAttacked(WorldObject attacker) {
   }

   @Override
   public void notifyActionAggression(WorldObject target, int aggro) {
   }

   @Override
   public void notifyActionStunned() {
   }

   @Override
   public void notifyActionSleeping() {
   }

   @Override
   public void notifyActionRooted() {
   }

   @Override
   public void notifyActionForgetObject(WorldObject object) {
   }

   @Override
   public void notifyActionCancel() {
   }

   @Override
   public void notifyActionDeath() {
   }

   @Override
   public void notifyActionFakeDeath() {
   }

   @Override
   public void notifyActionFinishCasting() {
   }

   @Override
   protected void clientActionFailed() {
   }

   @Override
   public void moveToPawn(WorldObject pawn, int offset) {
   }

   @Override
   protected void clientStoppedMoving() {
   }

   public AirShip getActor() {
      return (AirShip)this._actor;
   }
}
