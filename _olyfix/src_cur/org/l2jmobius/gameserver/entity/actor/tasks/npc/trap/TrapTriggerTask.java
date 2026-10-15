package org.l2jmobius.gameserver.entity.actor.tasks.npc.trap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;

public class TrapTriggerTask implements Runnable {
   private final Trap _trap;

   public TrapTriggerTask(Trap trap) {
      this._trap = trap;
   }

   @Override
   public void run() {
      try {
         this._trap.doCast(this._trap.getSkill());
         ThreadPool.schedule(new TrapUnsummonTask(this._trap), this._trap.getSkill().getHitTime() + 300);
      } catch (Exception var2) {
         this._trap.unSummon();
      }
   }
}
