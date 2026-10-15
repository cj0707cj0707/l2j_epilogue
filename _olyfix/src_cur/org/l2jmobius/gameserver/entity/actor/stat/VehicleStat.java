package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.Vehicle;

public class VehicleStat extends CreatureStat {
   private float _moveSpeed = 0.0F;
   private int _rotationSpeed = 0;

   public VehicleStat(Vehicle activeChar) {
      super(activeChar);
   }

   @Override
   public double getMoveSpeed() {
      return this._moveSpeed;
   }

   public void setMoveSpeed(float speed) {
      this._moveSpeed = speed;
   }

   public double getRotationSpeed() {
      return this._rotationSpeed;
   }

   public void setRotationSpeed(int speed) {
      this._rotationSpeed = speed;
   }
}
