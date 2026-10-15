package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition extends ClientPacket {
   private int _x;
   private int _y;
   private int _z;
   private int _heading;

   @Override
   protected void readImpl() {
      this._x = this.readInt();
      this._y = this.readInt();
      this._z = this.readInt();
      this._heading = this.readInt();
      this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && !player.isTeleporting() && !player.inObserverMode() && !player.isCastingNow()) {
         int realX = player.getX();
         if (this._x != 0 || this._y != 0 || realX == 0) {
            if (!player.isInVehicle()) {
               if (!player.isFalling(this._z)) {
                  if (this._z >= -20000 && this._z <= 20000) {
                     if (player.isFlyingMounted() && this._x > -166168) {
                        player.untransform();
                     }

                     int realY = player.getY();
                     int realZ = player.getZ();
                     int dx = this._x - realX;
                     int dy = this._y - realY;
                     int dz = this._z - realZ;
                     double diffSq = dx * dx + dy * dy;
                     if (player.isFlying() || player.isInsideZone(ZoneId.WATER)) {
                        player.setXYZ(realX, realY, this._z);
                        if (diffSq > 90000.0) {
                           player.sendPacket(new ValidateLocation(player));
                        }
                     } else if (diffSq < 360000.0 && (diffSq > 250000.0 || Math.abs(dz) > 200)) {
                        if (Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(this._z - player.getClientZ()) < 800) {
                           player.setXYZ(realX, realY, this._z);
                           realZ = this._z;
                        } else {
                           player.sendPacket(new ValidateLocation(player));
                        }
                     }

                     if (player.calculateDistance3D(this._x, this._y, this._z) > player.getStat().getMoveSpeed()) {
                        if (player.isBlinkActive()) {
                           ThreadPool.schedule(() -> player.setBlinkActive(false), 100L);
                        } else {
                           player.setXYZ(this._x, this._y, realZ > this._z ? GeoEngine.getInstance().getHeight(this._x, this._y, realZ) : this._z);
                        }
                     }

                     player.setClientX(this._x);
                     player.setClientY(this._y);
                     player.setClientZ(this._z);
                     player.setClientHeading(this._heading);
                     if (!DoorData.getInstance().checkIfDoorsBetween(realX, realY, realZ, this._x, this._y, this._z, player.getInstanceId(), false)) {
                        player.setLastServerPosition(realX, realY, realZ);
                     }

                     if (player.isCursorKeyMovement()) {
                        player.setSyncedXYZ(this._x, this._y, this._z);
                        player.broadcastPacket(new ValidateLocation(player));
                     }
                  } else {
                     Location lastServerPosition = player.getLastServerPosition();
                     if (lastServerPosition != null) {
                        player.teleToLocation(lastServerPosition);
                     }
                  }
               }
            }
         }
      }
   }
}
