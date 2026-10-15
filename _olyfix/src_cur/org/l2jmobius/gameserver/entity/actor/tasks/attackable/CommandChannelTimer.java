package org.l2jmobius.gameserver.entity.actor.tasks.attackable;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;

public class CommandChannelTimer implements Runnable {
   private final Attackable _attackable;

   public CommandChannelTimer(Attackable attackable) {
      this._attackable = attackable;
   }

   @Override
   public void run() {
      if (this._attackable != null) {
         if (System.currentTimeMillis() - this._attackable.getCommandChannelLastAttack() > PlayerConfig.LOOT_RAIDS_PRIVILEGE_INTERVAL) {
            this._attackable.setCommandChannelTimer(null);
            this._attackable.setFirstCommandChannelAttacked(null);
            this._attackable.setCommandChannelLastAttack(0L);
         } else {
            ThreadPool.schedule(this, 10000L);
         }
      }
   }
}
