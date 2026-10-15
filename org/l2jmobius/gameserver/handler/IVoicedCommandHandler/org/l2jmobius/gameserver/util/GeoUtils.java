package org.l2jmobius.gameserver.util;

import java.awt.Color;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

public class GeoUtils {
   public static void debug2DLine(Player player, int x, int y, int tx, int ty, int z) {
      int gx = GeoEngine.getGeoX(x);
      int gy = GeoEngine.getGeoY(y);
      int tgx = GeoEngine.getGeoX(tx);
      int tgy = GeoEngine.getGeoY(ty);
      ExServerPrimitive prim = new ExServerPrimitive("Debug2DLine", x, y, z);
      prim.addLine(Color.BLUE, GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), z, GeoEngine.getWorldX(tgx), GeoEngine.getWorldY(tgy), z);
      GridLineIterator2D iter = new GridLineIterator2D(gx, gy, tgx, tgy);

      while (iter.next()) {
         int wx = GeoEngine.getWorldX(iter.x());
         int wy = GeoEngine.getWorldY(iter.y());
         prim.addPoint(Color.RED, wx, wy, z);
      }

      player.sendPacket(prim);
   }

   public static void debug3DLine(Player player, int x, int y, int z, int tx, int ty, int tz) {
      int gx = GeoEngine.getGeoX(x);
      int gy = GeoEngine.getGeoY(y);
      int tgx = GeoEngine.getGeoX(tx);
      int tgy = GeoEngine.getGeoY(ty);
      ExServerPrimitive prim = new ExServerPrimitive("Debug3DLine", x, y, z);
      prim.addLine(Color.BLUE, GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), z, GeoEngine.getWorldX(tgx), GeoEngine.getWorldY(tgy), tz);
      GridLineIterator3D iter = new GridLineIterator3D(gx, gy, z, tgx, tgy, tz);
      iter.next();
      int prevX = iter.x();
      int prevY = iter.y();
      int wx = GeoEngine.getWorldX(prevX);
      int wy = GeoEngine.getWorldY(prevY);
      int wz = iter.z();
      prim.addPoint(Color.RED, wx, wy, wz);

      while (iter.next()) {
         int curX = iter.x();
         int curY = iter.y();
         if (curX != prevX || curY != prevY) {
            wx = GeoEngine.getWorldX(curX);
            wy = GeoEngine.getWorldY(curY);
            wz = iter.z();
            prim.addPoint(Color.RED, wx, wy, wz);
            prevX = curX;
            prevY = curY;
         }
      }

      player.sendPacket(prim);
   }

   private static Color getDirectionColor(int x, int y, int z, int nswe) {
      return GeoEngine.getInstance().checkNearestNswe(x, y, z, nswe) ? Color.GREEN : Color.RED;
   }

   public static void debugGrid(Player player) {
      int geoRadius = 20;
      int blocksPerPacket = 40;
      int iBlock = 40;
      int iPacket = 0;
      ExServerPrimitive exsp = null;
      int playerGx = GeoEngine.getGeoX(player.getX());
      int playerGy = GeoEngine.getGeoY(player.getY());

      for (int dx = -20; dx <= 20; dx++) {
         for (int dy = -20; dy <= 20; dy++) {
            if (iBlock >= 40) {
               iBlock = 0;
               if (exsp != null) {
                  iPacket++;
                  player.sendPacket(exsp);
               }

               exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
            }

            if (exsp == null) {
               throw new IllegalStateException();
            }

            int gx = playerGx + dx;
            int gy = playerGy + dy;
            int x = GeoEngine.getWorldX(gx);
            int y = GeoEngine.getWorldY(gy);
            int z = GeoEngine.getInstance().getNearestZ(gx, gy, player.getZ());
            Color col = getDirectionColor(gx, gy, z, 8);
            exsp.addLine(col, x - 1, y - 7, z, x + 1, y - 7, z);
            exsp.addLine(col, x - 2, y - 6, z, x + 2, y - 6, z);
            exsp.addLine(col, x - 3, y - 5, z, x + 3, y - 5, z);
            exsp.addLine(col, x - 4, y - 4, z, x + 4, y - 4, z);
            col = getDirectionColor(gx, gy, z, 1);
            exsp.addLine(col, x + 7, y - 1, z, x + 7, y + 1, z);
            exsp.addLine(col, x + 6, y - 2, z, x + 6, y + 2, z);
            exsp.addLine(col, x + 5, y - 3, z, x + 5, y + 3, z);
            exsp.addLine(col, x + 4, y - 4, z, x + 4, y + 4, z);
            col = getDirectionColor(gx, gy, z, 4);
            exsp.addLine(col, x - 1, y + 7, z, x + 1, y + 7, z);
            exsp.addLine(col, x - 2, y + 6, z, x + 2, y + 6, z);
            exsp.addLine(col, x - 3, y + 5, z, x + 3, y + 5, z);
            exsp.addLine(col, x - 4, y + 4, z, x + 4, y + 4, z);
            col = getDirectionColor(gx, gy, z, 2);
            exsp.addLine(col, x - 7, y - 1, z, x - 7, y + 1, z);
            exsp.addLine(col, x - 6, y - 2, z, x - 6, y + 2, z);
            exsp.addLine(col, x - 5, y - 3, z, x - 5, y + 3, z);
            exsp.addLine(col, x - 4, y - 4, z, x - 4, y + 4, z);
            iBlock++;
         }
      }

      player.sendPacket(exsp);
   }

   public static void hideDebugGrid(Player player) {
      int geoRadius = 20;
      int blocksPerPacket = 40;
      int iBlock = 40;
      int iPacket = 0;
      ExServerPrimitive exsp = null;
      int playerGx = GeoEngine.getGeoX(player.getX());
      int playerGy = GeoEngine.getGeoY(player.getY());

      for (int dx = -20; dx <= 20; dx++) {
         for (int dy = -20; dy <= 20; dy++) {
            if (iBlock >= 40) {
               iBlock = 0;
               if (exsp != null) {
                  iPacket++;
                  player.sendPacket(exsp);
               }

               exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
            }

            if (exsp == null) {
               throw new IllegalStateException();
            }

            int gx = playerGx + dx;
            int gy = playerGy + dy;
            int x = GeoEngine.getWorldX(gx);
            int y = GeoEngine.getWorldY(gy);
            exsp.addLine(Color.BLACK, x, y, -16000, x, y, -16000);
            iBlock++;
         }
      }

      player.sendPacket(exsp);
   }

   public static int computeNswe(int lastX, int lastY, int x, int y) {
      if (x > lastX) {
         if (y > lastY) {
            return 5;
         } else {
            return y < lastY ? 9 : 1;
         }
      } else if (x < lastX) {
         if (y > lastY) {
            return 6;
         } else {
            return y < lastY ? 10 : 2;
         }
      } else if (y > lastY) {
         return 4;
      } else if (y < lastY) {
         return 8;
      } else {
         throw new RuntimeException();
      }
   }
}
