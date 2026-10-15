package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class WarnUserTakeBreakTask implements Runnable {
   private final Player _player;

   public WarnUserTakeBreakTask(Player player) {
      this._player = player;
   }

   @Override
   public void run() {
      if (this._player != null) {
         if (this._player.isOnline()) {
            this._player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_PLAYING_FOR_AN_EXTENDED_PERIOD_OF_TIME_PLEASE_CONSIDER_TAKING_A_BREAK);
         } else {
            this._player.stopWarnUserTakeBreak();
         }
      }
   }
}
