package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.RaidBossPointsManager;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBossRecord;

public class RequestGetBossRecord extends ClientPacket {
   private int _bossId;

   @Override
   protected void readImpl() {
      this._bossId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._bossId != 0) {
            org.l2jmobius.gameserver.network.PacketLogger.info("C5: RequestGetBossRecord: d: " + this._bossId + " ActiveChar: " + player);
         }

         int points = RaidBossPointsManager.getInstance().getPointsByOwnerId(player.getObjectId());
         int ranking = RaidBossPointsManager.getInstance().calculateRanking(player.getObjectId());
         Map<Integer, Integer> list = RaidBossPointsManager.getInstance().getList(player);
         player.sendPacket(new ExGetBossRecord(ranking, points, list));
      }
   }
}
