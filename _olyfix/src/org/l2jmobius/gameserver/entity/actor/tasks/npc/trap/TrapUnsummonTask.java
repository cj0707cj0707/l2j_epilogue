package org.l2jmobius.gameserver.entity.actor.tasks.npc.trap;

import org.l2jmobius.gameserver.entity.actor.instance.Trap;

public class TrapUnsummonTask implements Runnable {
   private final Trap _trap;

   public TrapUnsummonTask(Trap trap) {
      this._trap = trap;
   }

   @Override
   public void run() {
      this._trap.unSummon();
   }
}
