package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;

public class ExAirShipInfo extends ServerPacket {
   private final AirShip _ship;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;
   private final int _moveSpeed;
   private final int _rotationSpeed;
   private final int _captain;
   private final int _helm;

   public ExAirShipInfo(AirShip ship) {
      this._ship = ship;
      this._x = ship.getX();
      this._y = ship.getY();
      this._z = ship.getZ();
      this._heading = ship.getHeading();
      this._moveSpeed = (int)ship.getStat().getMoveSpeed();
      this._rotationSpeed = (int)ship.getStat().getRotationSpeed();
      this._captain = ship.getCaptainId();
      this._helm = ship.getHelmObjectId();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_AIRSHIP_INFO.writeId(this, buffer);
      buffer.writeInt(this._ship.getObjectId());
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._heading);
      buffer.writeInt(this._captain);
      buffer.writeInt(this._moveSpeed);
      buffer.writeInt(this._rotationSpeed);
      buffer.writeInt(this._helm);
      if (this._helm != 0) {
         buffer.writeInt(366);
         buffer.writeInt(0);
         buffer.writeInt(107);
         buffer.writeInt(348);
         buffer.writeInt(0);
         buffer.writeInt(105);
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
      }

      buffer.writeInt(this._ship.getFuel());
      buffer.writeInt(this._ship.getMaxFuel());
   }
}
