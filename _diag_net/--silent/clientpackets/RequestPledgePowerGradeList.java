package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Collection;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.Clan.RankPrivs;
import org.l2jmobius.gameserver.network.serverpackets.PledgePowerGradeList;

public class RequestPledgePowerGradeList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      Clan clan = player.getClan();
      if (clan != null) {
         Collection<RankPrivs> privs = clan.getAllRankPrivs();
         player.sendPacket(new PledgePowerGradeList(privs));
      }
   }
}
