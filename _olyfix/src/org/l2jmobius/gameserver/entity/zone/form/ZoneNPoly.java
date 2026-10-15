package org.l2jmobius.gameserver.entity.zone.form;

import java.awt.Polygon;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class ZoneNPoly extends ZoneForm {
   private final Polygon _p;
   private final int _z1;
   private final int _z2;
   private final Location _centerPoint;

   public ZoneNPoly(int[] x, int[] y, int z1, int z2) {
      this._p = new Polygon(x, y, x.length);
      this._z1 = Math.min(z1, z2);
      this._z2 = Math.max(z1, z2);
      double area = 0.0;
      double cx = 0.0;
      double cy = 0.0;

      for (int i = 0; i < this._p.npoints; i++) {
         int nextIndex = (i + 1) % this._p.npoints;
         double crossProduct = this._p.xpoints[i] * this._p.ypoints[nextIndex] - this._p.xpoints[nextIndex] * this._p.ypoints[i];
         area += crossProduct;
         cx += (this._p.xpoints[i] + this._p.xpoints[nextIndex]) * crossProduct;
         cy += (this._p.ypoints[i] + this._p.ypoints[nextIndex]) * crossProduct;
      }

      area /= 2.0;
      if (area == 0.0) {
         cx = 0.0;
         cy = 0.0;

         for (int i = 0; i < this._p.npoints; i++) {
            cx += this._p.xpoints[i];
            cy += this._p.ypoints[i];
         }

         cx /= this._p.npoints;
         cy /= this._p.npoints;
      } else {
         cx /= 6.0 * area;
         cy /= 6.0 * area;
      }

      this._centerPoint = new Location((int)cx, (int)cy, (this._z1 + this._z2) / 2);
   }

   @Override
   public boolean isInsideZone(int x, int y, int z) {
      return this._p.contains(x, y) && z >= this._z1 && z <= this._z2;
   }

   @Override
   public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2) {
      return this._p.intersects(Math.min(ax1, ax2), Math.min(ay1, ay2), Math.abs(ax2 - ax1), Math.abs(ay2 - ay1));
   }

   @Override
   public double getDistanceToZone(int x, int y) {
      int[] xPoints = this._p.xpoints;
      int[] yPoints = this._p.ypoints;
      double dx = xPoints[0] - x;
      double dy = yPoints[0] - y;
      double shortestDist = dx * dx + dy * dy;

      for (int i = 1; i < this._p.npoints; i++) {
         dx = xPoints[i] - x;
         dy = yPoints[i] - y;
         double test = dx * dx + dy * dy;
         if (test < shortestDist) {
            shortestDist = test;
         }
      }

      return Math.sqrt(shortestDist);
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
      for (int i = 0; i < this._p.npoints; i++) {
         int nextIndex = i + 1 == this._p.xpoints.length ? 0 : i + 1;
         int vx = this._p.xpoints[nextIndex] - this._p.xpoints[i];
         int vy = this._p.ypoints[nextIndex] - this._p.ypoints[i];
         float length = (float)Math.sqrt(vx * vx + vy * vy) / 10.0F;

         for (int o = 1; o <= length; o++) {
            this.dropDebugItem(57, 1, (int)(this._p.xpoints[i] + o / length * vx), (int)(this._p.ypoints[i] + o / length * vy), z);
         }
      }
   }

   @Override
   public Location getRandomPoint() {
      int minX = this._p.getBounds().x;
      int maxX = this._p.getBounds().x + this._p.getBounds().width;
      int minY = this._p.getBounds().y;
      int maxY = this._p.getBounds().y + this._p.getBounds().height;
      int x = Rnd.get(minX, maxX);
      int y = Rnd.get(minY, maxY);

      for (int antiBlocker = 0; !this._p.contains(x, y) && antiBlocker++ < 1000; y = Rnd.get(minY, maxY)) {
         x = Rnd.get(minX, maxX);
      }

      return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (this._z1 + this._z2) / 2));
   }

   public int[] getX() {
      return this._p.xpoints;
   }

   public int[] getY() {
      return this._p.ypoints;
   }

   @Override
   public Location getCenterPoint() {
      return this._centerPoint;
   }
}
