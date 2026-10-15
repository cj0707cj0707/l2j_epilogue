package org.l2jmobius.gameserver.ai;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class DoorAI extends CreatureAI {
   public DoorAI(Door door) {
      super(door);
   }

   @Override
   public void setIntentionIdle() {
   }

   @Override
   public void setIntentionActive() {
   }

   @Override
   public void setIntentionRest() {
   }

   @Override
   public void setIntentionAttack(WorldObject target) {
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
   }

   @Override
   public void setIntentionMoveTo(ILocational destination) {
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
   public void notifyActionThink() {
   }

   @Override
   public void notifyActionAttacked(WorldObject attacker) {
      if (attacker != null) {
         Creature attackerCreature = attacker.asCreature();
         if (attackerCreature != null) {
            ThreadPool.execute(() -> World.forEachVisibleObject(this._actor.asDoor(), Defender.class, guard -> {
               if (this._actor.isInsideRadius3D(guard, guard.getTemplate().getClanHelpRange())) {
                  guard.getAI().notifyActionAggression(attackerCreature, 15);
               }
            }));
         }
      }
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
   public void notifyActionReadyToAct() {
   }

   @Override
   public void notifyActionUserCmd(Object arg0, Object arg1) {
   }

   @Override
   public void notifyActionArrived() {
   }

   @Override
   public void notifyActionArrivedRevalidate() {
   }

   @Override
   public void notifyActionArrivedBlocked(Location blockedAtLoc) {
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
}
