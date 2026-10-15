package org.l2jmobius.gameserver.geoengine.geodata;

public interface IRegion {
   int REGION_BLOCKS_X = 256;
   int REGION_BLOCKS_Y = 256;
   int REGION_BLOCKS = 65536;
   int REGION_CELLS_X = 2048;
   int REGION_CELLS_Y = 2048;
   int REGION_CELLS = 4194304;

   boolean checkNearestNswe(int var1, int var2, int var3, int var4);

   void setNearestNswe(int var1, int var2, int var3, byte var4);

   void unsetNearestNswe(int var1, int var2, int var3, byte var4);

   int getNearestZ(int var1, int var2, int var3);

   int getNextLowerZ(int var1, int var2, int var3);

   int getNextHigherZ(int var1, int var2, int var3);

   boolean hasGeo();

   boolean saveToFile(String var1);
}
