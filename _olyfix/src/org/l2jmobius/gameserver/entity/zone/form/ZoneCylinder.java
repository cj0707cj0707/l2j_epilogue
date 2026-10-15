package org.l2jmobius.gameserver.entity.zone.form;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class ZoneCylinder extends ZoneForm {
   private final int _x;
   private final int _y;
   private final int _z1;
   private final int _z2;
   private final int _rad;
   private final int _radS;
   private final Location _centerPoint;

   public ZoneCylinder(int x, int y, int z1, int z2, int rad) {
      this._x = x;
      this._y = y;
      this._z1 = z1;
      this._z2 = z2;
      this._rad = rad;
      this._radS = rad * rad;
      this._centerPoint = new Location(this._x, this._y, (this._z1 + this._z2) / 2);
   }

   @Override
   public boolean isInsideZone(int x, int y, int z) {
      double dx = this._x - x;
      double dy = this._y - y;
      return dx * dx + dy * dy <= this._radS && z >= this._z1 && z <= this._z2;
   }

   @Override
   public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2) {
      if (this._x > ax1 && this._x < ax2 && this._y > ay1 && this._y < ay2) {
         return true;
      } else {
         double dx1 = ax1 - this._x;
         double dy1 = ay1 - this._y;
         double dx1Sq = dx1 * dx1;
         double dy1Sq = dy1 * dy1;
         if (dx1Sq + dy1Sq < this._radS) {
            return true;
         } else {
            double dy2 = ay2 - this._y;
            double dy2Sq = dy2 * dy2;
            if (dx1Sq + dy2Sq < this._radS) {
               return true;
            } else {
               double dx2 = ax2 - this._x;
               double dx2Sq = dx2 * dx2;
               if (dx2Sq + dy1Sq < this._radS) {
                  return true;
               } else if (dx2Sq + dy2Sq < this._radS) {
                  return true;
               } else {
                  if (this._x > ax1 && this._x < ax2) {
                     if (Math.abs(this._y - ay2) < this._rad) {
                        return true;
                     }

                     if (Math.abs(this._y - ay1) < this._rad) {
                        return true;
                     }
                  }

                  if (this._y > ay1 && this._y < ay2) {
                     if (Math.abs(this._x - ax2) < this._rad) {
                        return true;
                     }

                     if (Math.abs(this._x - ax1) < this._rad) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         }
      }
   }

   @Override
   public double getDistanceToZone(int x, int y) {
      double dx = this._x - x;
      double dy = this._y - y;
      return Math.sqrt(dx * dx + dy * dy) - this._rad;
   }

   @Override
   public int getLowZ() {
      return this._z1;
   }

   @Override
   public int getHighZ() {
      return this._z2;
   }

   @Override
   public void visualizeZone(int z) {
      int count = (int)((Math.PI * 2) * this._rad / 10.0);
      double angle = (Math.PI * 2) / count;

      for (int i = 0; i < count; i++) {
         this.dropDebugItem(57, 1, this._x + (int)(Math.cos(angle * i) * this._rad), this._y + (int)(Math.sin(angle * i) * this._rad), z);
      }
   }

   @Override
   public Location getRandomPoint() {
      int x = 0;
      int y = 0;
      int x2 = this._x - this._rad;
      int y2 = this._y - this._rad;
      int x3 = this._x + this._rad;
      int y3 = this._y + this._rad;
      double dx = this._x - x;

      for (double dy = this._y - y; dx * dx + dy * dy > this._radS; dy = this._y - y) {
         x = Rnd.get(x2, x3);
         y = Rnd.get(y2, y3);
         dx = this._x - x;
      }

      return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (this._z1 + this._z2) / 2));
   }

   @Override
   public Location getCenterPoint() {
      return this._centerPoint;
   }
}
