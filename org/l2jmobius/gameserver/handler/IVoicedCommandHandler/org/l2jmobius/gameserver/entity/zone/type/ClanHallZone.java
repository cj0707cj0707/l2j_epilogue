package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.AgitDecoInfo;

public class ClanHallZone extends ResidenceZone {
   public ClanHallZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("clanHallId")) {
         this.setResidenceId(Integer.parseInt(value));
         ClanHall hall = ClanHallTable.getInstance().getClanHallById(this.getResidenceId());
         if (hall != null) {
            hall.setZone(this);
         } else {
            LOGGER.warning("ClanHallZone: Clan hall with id " + this.getResidenceId() + " does not exist!");
         }
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.CLAN_HALL, true);
         AuctionableHall clanHall = ClanHallTable.getInstance().getAuctionableHallById(this.getResidenceId());
         if (clanHall != null) {
            creature.sendPacket(new AgitDecoInfo(clanHall));
         }
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.CLAN_HALL, false);
      }
   }
}
