package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Tower;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

public class ControlTower extends Tower {
   private Collection<Spawn> _guards;

   public ControlTower(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.ControlTower);
   }

   @Override
   public boolean doDie(Creature killer) {
      if (this.getCastle().getSiege().isInProgress()) {
         this.getCastle().getSiege().killedCT();
         if (this._guards != null && !this._guards.isEmpty()) {
            for (Spawn spawn : this._guards) {
               try {
                  spawn.stopRespawn();
               } catch (Exception var5) {
                  LOGGER.log(Level.WARNING, "Error at ControlTower", (Throwable)var5);
               }
            }

            this._guards.clear();
         }
      }

      return super.doDie(killer);
   }

   public void registerGuard(Spawn guard) {
      this.getGuards().add(guard);
   }

   private Collection<Spawn> getGuards() {
      if (this._guards == null) {
         synchronized (this) {
            if (this._guards == null) {
               this._guards = ConcurrentHashMap.newKeySet();
            }
         }
      }

      return this._guards;
   }
}
