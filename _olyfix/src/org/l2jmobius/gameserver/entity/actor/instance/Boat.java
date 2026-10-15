package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.ai.BoatAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Vehicle;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.network.serverpackets.VehicleDeparture;
import org.l2jmobius.gameserver.network.serverpackets.VehicleInfo;
import org.l2jmobius.gameserver.network.serverpackets.VehicleStarted;

public class Boat extends Vehicle {
   protected static final Logger LOGGER_BOAT = Logger.getLogger(Boat.class.getName());

   public Boat(CreatureTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Boat);
      this.setAI(new BoatAI(this));
   }

   @Override
   public boolean isBoat() {
      return true;
   }

   @Override
   public int getId() {
      return 0;
   }

   @Override
   public boolean moveToNextRoutePoint() {
      boolean result = super.moveToNextRoutePoint();
      if (result) {
         this.broadcastPacket(new VehicleDeparture(this));
      }

      return result;
   }

   @Override
   public void oustPlayer(Player player) {
      super.oustPlayer(player);
      Location loc = this.getOustLoc();
      if (player.isOnline()) {
         player.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
      } else {
         player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
      }
   }

   @Override
   public void stopMove(Location loc) {
      super.stopMove(loc);
      this.broadcastPacket(new VehicleStarted(this, 0));
      this.broadcastPacket(new VehicleInfo(this));
   }

   @Override
   public void sendInfo(Player player) {
      player.sendPacket(new VehicleInfo(this));
   }
}
