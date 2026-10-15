package org.l2jmobius.gameserver.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;

public class PolygonTerritory {
   private static final Logger LOGGER = Logger.getLogger(PolygonTerritory.class.getName());
   private final List<PolygonTerritory.Point> _points = new CopyOnWriteArrayList<>();
   private final int _terr;
   private int _xMin;
   private int _xMax;
   private int _yMin;
   private int _yMax;
   private int _zMin;
   private int _zMax;
   private int _procMax;

   public PolygonTerritory(int terr) {
      this._terr = terr;
      this._xMin = 999999;
      this._xMax = -999999;
      this._yMin = 999999;
      this._yMax = -999999;
      this._zMin = 999999;
      this._zMax = -999999;
      this._procMax = 0;
   }

   public void add(int x, int y, int zmin, int zmax, int proc) {
      this._points.add(new PolygonTerritory.Point(x, y, zmin, zmax, proc));
      if (x < this._xMin) {
         this._xMin = x;
      }

      if (y < this._yMin) {
         this._yMin = y;
      }

      if (x > this._xMax) {
         this._xMax = x;
      }

      if (y > this._yMax) {
         this._yMax = y;
      }

      if (zmin < this._zMin) {
         this._zMin = zmin;
      }

      if (zmax > this._zMax) {
         this._zMax = zmax;
      }

      this._procMax += proc;
   }

   public boolean isIntersect(int x, int y, PolygonTerritory.Point p1, PolygonTerritory.Point p2) {
      double dy1 = p1._y - y;
      double dy2 = p2._y - y;
      if (Math.abs(Math.signum(dy1) - Math.signum(dy2)) <= 1.0E-6) {
         return false;
      } else {
         double dx1 = p1._x - x;
         double dx2 = p2._x - x;
         if (dx1 >= 0.0 && dx2 >= 0.0) {
            return true;
         } else if (dx1 < 0.0 && dx2 < 0.0) {
            return false;
         } else {
            double dx0 = dy1 * (p1._x - p2._x) / (p1._y - p2._y);
            return dx0 <= dx1;
         }
      }
   }

   public boolean isInside(int x, int y) {
      int intersectCount = 0;

      for (int i = 0; i < this._points.size(); i++) {
         PolygonTerritory.Point p1 = this._points.get(i > 0 ? i - 1 : this._points.size() - 1);
         PolygonTerritory.Point p2 = this._points.get(i);
         if (this.isIntersect(x, y, p1, p2)) {
            intersectCount++;
         }
      }

      return intersectCount % 2 == 1;
   }

   public Location getRandomPoint() {
      if (this._procMax > 0) {
         int pos = 0;
         int rnd = Rnd.get(this._procMax);

         for (PolygonTerritory.Point p1 : this._points) {
            pos += p1._proc;
            if (rnd <= pos) {
               return new Location(p1._x, p1._y, Rnd.get(p1._zmin, p1._zmax));
            }
         }
      }

      for (int i = 0; i < 100; i++) {
         int x = Rnd.get(this._xMin, this._xMax);
         int y = Rnd.get(this._yMin, this._yMax);
         if (this.isInside(x, y)) {
            double curdistance = 0.0;
            int zmin = this._zMin;

            for (PolygonTerritory.Point p1x : this._points) {
               double dx = p1x._x - x;
               double dy = p1x._y - y;
               double distance = Math.sqrt(dx * dx + dy * dy);
               if (curdistance == 0.0 || distance < curdistance) {
                  curdistance = distance;
                  zmin = p1x._zmin;
               }
            }

            return new Location(x, y, Rnd.get(zmin, this._zMax));
         }
      }

      LOGGER.warning("Can't make point for territory " + this._terr);
      return null;
   }

   public int getProcMax() {
      return this._procMax;
   }

   protected static class Point {
      protected int _x;
      protected int _y;
      protected int _zmin;
      protected int _zmax;
      protected int _proc;

      Point(int x, int y, int zmin, int zmax, int proc) {
         this._x = x;
         this._y = y;
         this._zmin = zmin;
         this._zmax = zmax;
         this._proc = proc;
      }
   }
}
