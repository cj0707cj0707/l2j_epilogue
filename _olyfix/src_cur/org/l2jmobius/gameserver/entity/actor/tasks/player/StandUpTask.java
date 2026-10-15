package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.entity.actor.Player;

public class StandUpTask implements Runnable {
   private final Player _player;

   public StandUpTask(Player player) {
      this._player = player;
   }

   @Override
   public void run() {
      if (this._player != null) {
         this._player.setParalyzed(false);
         this._player.setSitting(false);
         this._player.setSittingProgress(false);
         this._player.getAI().setIntentionIdle();
      }
   }
}
