package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.Location;

public class VehiclePathPoint extends Location {
   private final int _moveSpeed;
   private final int _rotationSpeed;

   public VehiclePathPoint(int x, int y, int z) {
      super(x, y, z);
      this._moveSpeed = 350;
      this._rotationSpeed = 4000;
   }

   public VehiclePathPoint(int x, int y, int z, int moveSpeed, int rotationSpeed) {
      super(x, y, z);
      this._moveSpeed = moveSpeed;
      this._rotationSpeed = rotationSpeed;
   }

   public int getMoveSpeed() {
      return this._moveSpeed;
   }

   public int getRotationSpeed() {
      return this._rotationSpeed;
   }
}
