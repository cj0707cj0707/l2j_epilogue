package org.l2jmobius.gameserver.network.clientpackets;

public class ExGetOnAirShip extends ClientPacket {
   private int _x;
   private int _y;
   private int _z;
   private int _shipId;

   @Override
   protected void readImpl() {
      this._x = this.readInt();
      this._y = this.readInt();
      this._z = this.readInt();
      this._shipId = this.readInt();
   }

   @Override
   protected void runImpl() {
   }
}
