package org.l2jmobius.gameserver.entity.actor.tasks.npc.walker;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkInfo;
import org.l2jmobius.gameserver.managers.WalkingManager;

public class ArrivedTask implements Runnable {
   private final WalkInfo _walk;
   private final Npc _npc;

   public ArrivedTask(Npc npc, WalkInfo walk) {
      this._npc = npc;
      this._walk = walk;
   }

   @Override
   public void run() {
      this._walk.setBlocked(false);
      WalkingManager.getInstance().startMoving(this._npc, this._walk.getRoute().getName());
   }
}
