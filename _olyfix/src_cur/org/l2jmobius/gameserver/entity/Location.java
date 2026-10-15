package org.l2jmobius.gameserver.entity;

import java.util.Objects;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.util.StatSet;

public class Location implements IPositionable {
   private static final int MAX_HEADING = 65535;
   protected volatile int _x;
   protected volatile int _y;
   protected volatile int _z;
   protected volatile int _heading;
   protected volatile int _instanceId;

   public Location(int x, int y, int z) {
      this._x = x;
      this._y = y;
      this._z = z;
      this._heading = 0;
      this._instanceId = 0;
   }

   public Location(int x, int y, int z, int heading) {
      this._x = x;
      this._y = y;
      this._z = z;
      this._heading = heading;
      this._instanceId = 0;
   }

   public Location(WorldObject obj) {
      this(obj.getX(), obj.getY(), obj.getZ(), obj.getHeading(), obj.getInstanceId());
   }

   public Location(int x, int y, int z, int heading, int instanceId) {
      this._x = x;
      this._y = y;
      this._z = z;
      this._heading = heading;
      this._instanceId = instanceId;
   }

   public Location(StatSet set) {
      this._x = set.getInt("x", 0);
      this._y = set.getInt("y", 0);
      this._z = set.getInt("z", 0);
      this._heading = set.getInt("heading", 0);
      this._instanceId = set.getInt("instanceId", 0);
   }

   @Override
   public int getX() {
      return this._x;
   }

   @Override
   public int getY() {
      return this._y;
   }

   @Override
   public int getZ() {
      return this._z;
   }

   @Override
   public void setXYZ(int x, int y, int z) {
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void setXYZ(ILocational loc) {
      this.setXYZ(loc.getX(), loc.getY(), loc.getZ());
   }

   @Override
   public int getHeading() {
      return this._heading;
   }

   @Override
   public void setHeading(int heading) {
      this._heading = Math.clamp((long)heading, 0, 65535);
   }

   @Override
   public int getInstanceId() {
      return this._instanceId;
   }

   @Override
   public void setInstanceId(int instanceId) {
      this._instanceId = instanceId;
   }

   public IPositionable getLocation() {
      return this;
   }

   @Override
   public void setLocation(Location loc) {
      this._x = loc.getX();
      this._y = loc.getY();
      this._z = loc.getZ();
      this._heading = loc.getHeading();
      this._instanceId = loc.getInstanceId();
   }

   public Location clone() {
      return new Location(this._x, this._y, this._z, this._heading, this._instanceId);
   }

   @Override
   public int hashCode() {
      return 31 * Objects.hash(this._x, this._y) + Objects.hash(this._z);
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof Location loc)
         ? false
         : this.getX() == loc.getX()
            && this.getY() == loc.getY()
            && this.getZ() == loc.getZ()
            && this.getHeading() == loc.getHeading()
            && this.getInstanceId() == loc.getInstanceId();
   }

   @Override
   public String toString() {
      return "["
         + this.getClass().getSimpleName()
         + "] X: "
         + this._x
         + " Y: "
         + this._y
         + " Z: "
         + this._z
         + " Heading: "
         + this._heading
         + " InstanceId: "
         + this._instanceId;
   }
}
