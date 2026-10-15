package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.ai.AirShipAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Vehicle;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.network.serverpackets.ExAirShipInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOffAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExStopMoveAirShip;

public class AirShip extends Vehicle {
   public AirShip(CreatureTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.AirShip);
      this.setAI(new AirShipAI(this));
   }

   @Override
   public boolean isAirShip() {
      return true;
   }

   public boolean isOwner(Player player) {
      return false;
   }

   public int getOwnerId() {
      return 0;
   }

   public boolean isCaptain(Player player) {
      return false;
   }

   public int getCaptainId() {
      return 0;
   }

   public int getHelmObjectId() {
      return 0;
   }

   public int getHelmItemId() {
      return 0;
   }

   public boolean setCaptain(Player player) {
      return false;
   }

   public int getFuel() {
      return 0;
   }

   public void setFuel(int f) {
   }

   public int getMaxFuel() {
      return 0;
   }

   public void setMaxFuel(int mf) {
   }

   @Override
   public int getId() {
      return 0;
   }

   @Override
   public boolean moveToNextRoutePoint() {
      boolean result = super.moveToNextRoutePoint();
      if (result) {
         this.broadcastPacket(new ExMoveToLocationAirShip(this));
      }

      return result;
   }

   @Override
   public boolean addPassenger(Player player) {
      if (!super.addPassenger(player)) {
         return false;
      } else {
         player.setVehicle(this);
         player.setInVehiclePosition(new Location(0, 0, 0));
         player.broadcastPacket(new ExGetOnAirShip(player, this));
         player.setXYZ(this.getX(), this.getY(), this.getZ());
         player.revalidateZone(true);
         player.stopMove(null);
         return true;
      }
   }

   @Override
   public void oustPlayer(Player player) {
      super.oustPlayer(player);
      Location loc = this.getOustLoc();
      if (player.isOnline()) {
         player.broadcastPacket(new ExGetOffAirShip(player, this, loc.getX(), loc.getY(), loc.getZ()));
         player.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
      } else {
         player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
      }
   }

   @Override
   public boolean deleteMe() {
      if (!super.deleteMe()) {
         return false;
      } else {
         AirShipManager.getInstance().removeAirShip(this);
         return true;
      }
   }

   @Override
   public void stopMove(Location loc) {
      super.stopMove(loc);
      this.broadcastPacket(new ExStopMoveAirShip(this));
   }

   @Override
   public void updateAbnormalEffect() {
      this.broadcastPacket(new ExAirShipInfo(this));
   }

   @Override
   public void sendInfo(Player player) {
      if (this.isVisibleFor(player)) {
         player.sendPacket(new ExAirShipInfo(this));
      }
   }
}
