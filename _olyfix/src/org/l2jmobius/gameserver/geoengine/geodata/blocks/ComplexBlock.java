package org.l2jmobius.gameserver.geoengine.geodata.blocks;

import java.nio.ByteBuffer;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;

public class ComplexBlock implements IBlock {
   private final short[] _data = new short[64];

   public ComplexBlock(ByteBuffer bb) {
      for (int cellOffset = 0; cellOffset < 64; cellOffset++) {
         this._data[cellOffset] = bb.getShort();
      }
   }

   private short getCellData(int geoX, int geoY) {
      return this._data[geoX % 8 * 8 + geoY % 8];
   }

   private byte getCellNSWE(int geoX, int geoY) {
      return (byte)(this.getCellData(geoX, geoY) & 15);
   }

   private int getCellHeight(int geoX, int geoY) {
      return (short)(this.getCellData(geoX, geoY) & 65520) >> 1;
   }

   @Override
   public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
      return (this.getCellNSWE(geoX, geoY) & nswe) == nswe;
   }

   @Override
   public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      short currentNswe = this.getNearestNswe(geoX, geoY, worldZ);
      if ((currentNswe & nswe) == 0) {
         short currentHeight = (short)this.getCellHeight(geoX, geoY);
         short encodedHeight = (short)(currentHeight << 1);
         short newNswe = (short)(currentNswe | nswe);
         short newCombinedData = (short)(encodedHeight | newNswe);
         this._data[geoX % 8 * 8 + geoY % 8] = (short)(newCombinedData & '\uffff');
      }
   }

   @Override
   public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      short currentNswe = this.getNearestNswe(geoX, geoY, worldZ);
      if ((currentNswe & nswe) != 0) {
         short currentHeight = (short)this.getCellHeight(geoX, geoY);
         short encodedHeight = (short)(currentHeight << 1);
         short newNswe = (short)(currentNswe & ~nswe);
         short newCombinedData = (short)(encodedHeight | newNswe);
         this._data[geoX % 8 * 8 + geoY % 8] = (short)(newCombinedData & '\uffff');
      }
   }

   @Override
   public short getNearestNswe(int geoX, int geoY, int worldZ) {
      short nswe = 0;
      if (this.checkNearestNswe(geoX, geoY, worldZ, 8)) {
         nswe = (short)(nswe | 8);
      }

      if (this.checkNearestNswe(geoX, geoY, worldZ, 1)) {
         nswe = (short)(nswe | 1);
      }

      if (this.checkNearestNswe(geoX, geoY, worldZ, 4)) {
         nswe = (short)(nswe | 4);
      }

      if (this.checkNearestNswe(geoX, geoY, worldZ, 2)) {
         nswe = (short)(nswe | 2);
      }

      return nswe;
   }

   @Override
   public int getNearestZ(int geoX, int geoY, int worldZ) {
      return this.getCellHeight(geoX, geoY);
   }

   @Override
   public int getNextLowerZ(int geoX, int geoY, int worldZ) {
      int cellHeight = this.getCellHeight(geoX, geoY);
      return cellHeight <= worldZ ? cellHeight : worldZ;
   }

   @Override
   public int getNextHigherZ(int geoX, int geoY, int worldZ) {
      int cellHeight = this.getCellHeight(geoX, geoY);
      return cellHeight >= worldZ ? cellHeight : worldZ;
   }

   public short[] getData() {
      return this._data;
   }
}
