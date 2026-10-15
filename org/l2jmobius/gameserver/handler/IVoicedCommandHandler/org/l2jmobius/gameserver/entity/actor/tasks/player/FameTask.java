package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class FameTask implements Runnable {
   private final Player _player;
   private final int _value;

   public FameTask(Player player, int value) {
      this._player = player;
      this._value = value;
   }

   @Override
   public void run() {
      if (this._player != null && (!this._player.isDead() || PlayerConfig.FAME_FOR_DEAD_PLAYERS)) {
         if (this._player.getClient() != null && !this._player.getClient().isDetached() || OfflineTradeConfig.OFFLINE_FAME) {
            this._player.setFame(this._player.getFame() + this._value);
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_REPUTATION_SCORE);
            sm.addInt(this._value);
            this._player.sendPacket(sm);
            this._player.updateUserInfo();
         }
      }
   }
}
