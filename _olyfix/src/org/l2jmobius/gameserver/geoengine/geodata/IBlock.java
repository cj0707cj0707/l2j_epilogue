package org.l2jmobius.gameserver.geoengine.geodata;

public interface IBlock {
   int TYPE_FLAT = 0;
   int TYPE_COMPLEX = 1;
   int TYPE_MULTILAYER = 2;
   int BLOCK_CELLS_X = 8;
   int BLOCK_CELLS_Y = 8;
   int BLOCK_CELLS = 64;

   boolean checkNearestNswe(int var1, int var2, int var3, int var4);

   void setNearestNswe(int var1, int var2, int var3, byte var4);

   void unsetNearestNswe(int var1, int var2, int var3, byte var4);

   short getNearestNswe(int var1, int var2, int var3);

   int getNearestZ(int var1, int var2, int var3);

   int getNextLowerZ(int var1, int var2, int var3);

   int getNextHigherZ(int var1, int var2, int var3);
}
