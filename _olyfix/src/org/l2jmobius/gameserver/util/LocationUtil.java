package org.l2jmobius.gameserver.util;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.interfaces.ILocational;

public class LocationUtil {
   public static double calculateAngleFrom(ILocational origin, ILocational target) {
      return calculateAngleFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
   }

   public static double calculateAngleFrom(int originX, int originY, int targetX, int targetY) {
      double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
      if (angle < 0.0) {
         angle += 360.0;
      }

      return angle;
   }

   public static int calculateHeadingFrom(ILocational origin, ILocational target) {
      return calculateHeadingFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
   }

   public static int calculateHeadingFrom(int originX, int originY, int targetX, int targetY) {
      double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
      if (angle < 0.0) {
         angle += 360.0;
      }

      return (int)(angle * 182.044444444);
   }

   public static int calculateHeadingFrom(double deltaX, double deltaY) {
      double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
      if (angle < 0.0) {
         angle += 360.0;
      }

      return (int)(angle * 182.044444444);
   }

   public static double convertHeadingToDegree(int clientHeading) {
      return clientHeading / 182.044444444;
   }

   public static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2, boolean includeZ, boolean squared) {
      double dx = x1 - x2;
      double dy = y1 - y2;
      double dz = z1 - z2;
      double distance = dx * dx + dy * dy + (includeZ ? dz * dz : 0.0);
      return squared ? distance : Math.sqrt(distance);
   }

   public static double calculateDistance(ILocational loc1, ILocational loc2, boolean includeZ, boolean squared) {
      return calculateDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), includeZ, squared);
   }

   public static boolean checkIfInRange(int range, WorldObject obj1, WorldObject obj2, boolean includeZ) {
      if (obj1 != null && obj2 != null && obj1.getInstanceId() == obj2.getInstanceId()) {
         int combinedRadius = 0;
         if (obj1.isCreature()) {
            combinedRadius += obj1.asCreature().getTemplate().getCollisionRadius();
         }

         if (obj2.isCreature()) {
            combinedRadius += obj2.asCreature().getTemplate().getCollisionRadius();
         }

         return calculateDistance(obj1, obj2, includeZ, false) <= range + combinedRadius;
      } else {
         return false;
      }
   }

   public static boolean isInsideRangeOfObjectId(WorldObject obj, int targetObjId, int radius) {
      WorldObject target = World.findObject(targetObjId);
      return target != null && obj.calculateDistance3D(target) <= radius;
   }

   public static Location getRandomLocation(ILocational center, int minRadius, int maxRadius) {
      int randomX = Rnd.get(minRadius, maxRadius);
      int randomY = Rnd.get(minRadius, maxRadius);
      double angle = Math.toRadians(Rnd.get(360));
      int newX = (int)(center.getX() + randomX * Math.cos(angle));
      int newY = (int)(center.getY() + randomY * Math.sin(angle));
      return new Location(newX, newY, center.getZ());
   }
}
