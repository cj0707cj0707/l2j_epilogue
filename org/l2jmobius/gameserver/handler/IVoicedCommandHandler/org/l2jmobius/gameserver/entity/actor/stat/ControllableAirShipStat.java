package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;

public class ControllableAirShipStat extends VehicleStat {
   public ControllableAirShipStat(ControllableAirShip activeChar) {
      super(activeChar);
   }

   public ControllableAirShip getActiveChar() {
      return (ControllableAirShip)super.getActiveChar();
   }

   @Override
   public double getMoveSpeed() {
      return !this.getActiveChar().isInDock() && this.getActiveChar().getFuel() <= 0 ? super.getMoveSpeed() * 0.05F : super.getMoveSpeed();
   }
}
