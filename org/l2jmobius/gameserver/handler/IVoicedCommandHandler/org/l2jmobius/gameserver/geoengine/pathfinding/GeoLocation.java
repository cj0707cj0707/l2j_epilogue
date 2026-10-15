package org.l2jmobius.gameserver.geoengine.pathfinding;

import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class GeoLocation {
   private int _x;
   private int _y;
   private int _nswe;
   private int _geoHeight;

   public GeoLocation(int x, int y, int z) {
      this.set(x, y, z);
   }

   public void set(int x, int y, int z) {
      this._x = x;
      this._y = y;
      this._nswe = 0;
      GeoEngine geoEngine = GeoEngine.getInstance();
      if (geoEngine.checkNearestNswe(x, y, z, 8)) {
         this._nswe |= 8;
      }

      if (geoEngine.checkNearestNswe(x, y, z, 1)) {
         this._nswe |= 1;
      }

      if (geoEngine.checkNearestNswe(x, y, z, 4)) {
         this._nswe |= 4;
      }

      if (geoEngine.checkNearestNswe(x, y, z, 2)) {
         this._nswe |= 2;
      }

      this._geoHeight = geoEngine.getNearestZ(x, y, z);
   }

   public boolean canGoNorth() {
      return (this._nswe & 8) != 0;
   }

   public boolean canGoEast() {
      return (this._nswe & 1) != 0;
   }

   public boolean canGoSouth() {
      return (this._nswe & 4) != 0;
   }

   public boolean canGoWest() {
      return (this._nswe & 2) != 0;
   }

   public boolean canGoNone() {
      return this._nswe == 0;
   }

   public boolean canGoAll() {
      return this._nswe == 15;
   }

   public int getX() {
      return GeoEngine.getWorldX(this._x);
   }

   public int getY() {
      return GeoEngine.getWorldY(this._y);
   }

   public int getZ() {
      return this._geoHeight;
   }

   public void setZ(short z) {
   }

   public int getNodeX() {
      return this._x;
   }

   public int getNodeY() {
      return this._y;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + this._x;
      result = 31 * result + this._y;
      return 31 * result + ((this._geoHeight & 65535) << 1 | this._nswe);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         GeoLocation other = (GeoLocation)obj;
         return this._x == other._x && this._y == other._y && this._geoHeight == other._geoHeight && this._nswe == other._nswe;
      } else {
         return false;
      }
   }
}
