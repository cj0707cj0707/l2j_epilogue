package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;

public class CastleDoorman extends Doorman {
   public CastleDoorman(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.CastleDoorman);
   }

   @Override
   protected void openDoors(Player player, String command) {
      StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
      st.nextToken();

      while (st.hasMoreTokens()) {
         if (this.getConquerableHall() != null) {
            this.getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), true);
         } else {
            this.getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
         }
      }
   }

   @Override
   protected void closeDoors(Player player, String command) {
      StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
      st.nextToken();

      while (st.hasMoreTokens()) {
         if (this.getConquerableHall() != null) {
            this.getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), false);
         } else {
            this.getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
         }
      }
   }

   @Override
   protected boolean isOwnerClan(Player player) {
      if (player.getClan() != null && player.hasAccess(ClanAccess.CASTLE_OPEN_DOOR)) {
         SiegableHall hall = this.getConquerableHall();
         if (hall != null) {
            if (player.getClanId() == hall.getOwnerId()) {
               return true;
            }
         } else if (this.getCastle() != null && player.getClanId() == this.getCastle().getOwnerId()) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected boolean isUnderSiege() {
      SiegableHall hall = this.getConquerableHall();
      return hall != null ? hall.isInSiege() : this.getCastle().getZone().isActive();
   }
}
