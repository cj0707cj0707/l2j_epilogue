package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class UCPoint {
   private final Location _loc;
   private final List<Door> _doors;
   private final List<Player> _players = new ArrayList<>();

   public UCPoint(List<Door> doors, Location loc) {
      this._doors = doors;
      this._loc = loc;
   }

   public void teleportPlayer(Player player) {
      if (player != null) {
         player.getVariables().set("RESTORE_LOCATION", player.getLocation().getX() + ";" + player.getLocation().getY() + ";" + player.getLocation().getZ());
         if (player.isDead()) {
            UCTeam.resPlayer(player);
         }

         player.teleToLocation(this._loc, true);
         this._players.add(player);
      }
   }

   public void actionDoors(boolean open) {
      if (!this._doors.isEmpty()) {
         for (Door door : this._doors) {
            if (open) {
               door.openMe();
            } else {
               door.closeMe();
            }
         }
      }
   }

   public Location getLocation() {
      return this._loc;
   }

   public List<Player> getPlayers() {
      return this._players;
   }

   public boolean checkPlayer(Player player) {
      if (this._players.contains(player)) {
         this.actionDoors(true);

         for (Player pl : this._players) {
            if (pl != null) {
               pl.setUCState(2);
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
