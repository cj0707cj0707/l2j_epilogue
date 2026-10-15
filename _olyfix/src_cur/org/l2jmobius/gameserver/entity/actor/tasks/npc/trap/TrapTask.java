package org.l2jmobius.gameserver.entity.actor.tasks.npc.trap;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class TrapTask implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(TrapTask.class.getName());
   private static final int TICK = 1000;
   private final Trap _trap;

   public TrapTask(Trap trap) {
      this._trap = trap;
   }

   @Override
   public void run() {
      try {
         if (!this._trap.isTriggered()) {
            if (this._trap.hasLifeTime()) {
               this._trap.setRemainingTime(this._trap.getRemainingTime() - 1000);
               if (this._trap.getRemainingTime() < this._trap.getLifeTime() - 15000) {
                  this._trap.broadcastPacket(new SocialAction(this._trap.getObjectId(), 2));
               }

               if (this._trap.getRemainingTime() <= 0) {
                  switch (this._trap.getSkill().getTargetType()) {
                     case AURA:
                     case FRONT_AURA:
                     case BEHIND_AURA:
                        this._trap.triggerTrap(this._trap);
                        break;
                     default:
                        this._trap.unSummon();
                  }

                  return;
               }
            }

            World.forFirstVisibleObject(this._trap, Creature.class, target -> this._trap.checkTarget(target), target -> this._trap.triggerTrap(target));
         }
      } catch (Exception var2) {
         LOGGER.severe(TrapTask.class.getSimpleName() + ": " + var2.getMessage());
         this._trap.unSummon();
      }
   }
}
