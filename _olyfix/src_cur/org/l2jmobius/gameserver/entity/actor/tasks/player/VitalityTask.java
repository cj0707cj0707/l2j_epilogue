package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityPointInfo;

public class VitalityTask implements Runnable {
   private final Player _player;

   public VitalityTask(Player player) {
      this._player = player;
   }

   @Override
   public void run() {
      if (this._player.isInsideZone(ZoneId.PEACE)) {
         if (this._player.getVitalityPoints() < PlayerStat.MAX_VITALITY_POINTS) {
            this._player.updateVitalityPoints(RatesConfig.RATE_RECOVERY_VITALITY_PEACE_ZONE, false, false);
            this._player.sendPacket(new ExVitalityPointInfo(this._player.getVitalityPoints()));
         }
      }
   }
}
