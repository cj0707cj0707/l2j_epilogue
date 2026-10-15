package org.l2jmobius.gameserver.geoengine.geodata.blocks;

import java.nio.ByteBuffer;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;

public class FlatBlock implements IBlock {
   private final short _height;

   public FlatBlock(ByteBuffer bb) {
      this._height = bb.getShort();
   }

   @Override
   public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
      return true;
   }

   @Override
   public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      throw new RuntimeException("Cannot set NSWE on a flat block!");
   }

   @Override
   public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      throw new RuntimeException("Cannot unset NSWE on a flat block!");
   }

   @Override
   public short getNearestNswe(int geoX, int geoY, int worldZ) {
      return 15;
   }

   @Override
   public int getNearestZ(int geoX, int geoY, int worldZ) {
      return this._height;
   }

   @Override
   public int getNextLowerZ(int geoX, int geoY, int worldZ) {
      return this._height <= worldZ ? this._height : worldZ;
   }

   @Override
   public int getNextHigherZ(int geoX, int geoY, int worldZ) {
      return this._height >= worldZ ? this._height : worldZ;
   }

   public short getHeight() {
      return this._height;
   }
}
