package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.managers.AirShipManager;

public class MoveToLocationAirShip extends ClientPacket {
   public static final int MIN_Z = -895;
   public static final int MAX_Z = 6105;
   public static final int STEP = 300;
   private int _command;
   private int _param1;
   private int _param2 = 0;

   @Override
   protected void readImpl() {
      this._command = this.readInt();
      this._param1 = this.readInt();
      if (this.remaining() > 0) {
         this._param2 = this.readInt();
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isInAirShip()) {
            AirShip ship = player.getAirShip();
            if (ship.isCaptain(player)) {
               int z = ship.getZ();
               switch (this._command) {
                  case 0:
                     if (!ship.canBeControlled()) {
                        return;
                     }

                     if (this._param1 < -166168) {
                        ship.getAI().setIntentionMoveTo(new Location(this._param1, this._param2, z));
                     }
                     break;
                  case 1:
                     if (!ship.canBeControlled()) {
                        return;
                     }

                     ship.getAI().setIntentionActive();
                     break;
                  case 2:
                     if (!ship.canBeControlled()) {
                        return;
                     }

                     if (z < 6105) {
                        z = Math.min(z + 300, 6105);
                        ship.getAI().setIntentionMoveTo(new Location(ship.getX(), ship.getY(), z));
                     }
                     break;
                  case 3:
                     if (!ship.canBeControlled()) {
                        return;
                     }

                     if (z > -895) {
                        z = Math.max(z - 300, -895);
                        ship.getAI().setIntentionMoveTo(new Location(ship.getX(), ship.getY(), z));
                     }
                     break;
                  case 4:
                     if (!ship.isInDock() || ship.isMoving()) {
                        return;
                     }

                     VehiclePathPoint[] dst = AirShipManager.getInstance().getTeleportDestination(ship.getDockId(), this._param1);
                     if (dst == null) {
                        return;
                     }

                     int fuelConsumption = AirShipManager.getInstance().getFuelConsumption(ship.getDockId(), this._param1);
                     if (fuelConsumption > 0) {
                        if (fuelConsumption > ship.getFuel()) {
                           player.sendPacket(
                              org.l2jmobius.gameserver.network.SystemMessageId.YOUR_SHIP_CANNOT_TELEPORT_BECAUSE_IT_DOES_NOT_HAVE_ENOUGH_FUEL_FOR_THE_TRIP
                           );
                           return;
                        }

                        ship.setFuel(ship.getFuel() - fuelConsumption);
                     }

                     ship.executePath(dst);
               }
            }
         }
      }
   }
}
