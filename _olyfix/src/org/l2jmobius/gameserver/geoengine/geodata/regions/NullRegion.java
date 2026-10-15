package org.l2jmobius.gameserver.geoengine.geodata.regions;

import org.l2jmobius.gameserver.geoengine.geodata.IRegion;

public class NullRegion implements IRegion {
   public static final NullRegion INSTANCE = new NullRegion();

   @Override
   public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
      return true;
   }

   @Override
   public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
   }

   @Override
   public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
   }

   @Override
   public int getNearestZ(int geoX, int geoY, int worldZ) {
      return worldZ;
   }

   @Override
   public int getNextLowerZ(int geoX, int geoY, int worldZ) {
      return worldZ;
   }

   @Override
   public int getNextHigherZ(int geoX, int geoY, int worldZ) {
      return worldZ;
   }

   @Override
   public boolean hasGeo() {
      return false;
   }

   @Override
   public boolean saveToFile(String fileName) {
      return false;
   }
}
