package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class WaterTask implements Runnable {
   private final Player _player;

   public WaterTask(Player player) {
      this._player = player;
   }

   @Override
   public void run() {
      if (this._player != null) {
         double reduceHp = this._player.getMaxHp() / 100.0 < 1.0 ? 1.0 : this._player.getMaxHp() / 100.0;
         this._player.reduceCurrentHp(reduceHp, this._player, false, false, null);
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_TAKEN_S1_DAMAGE_BECAUSE_YOU_WERE_UNABLE_TO_BREATHE);
         sm.addInt((int)reduceHp);
         this._player.sendPacket(sm);
      }
   }
}
