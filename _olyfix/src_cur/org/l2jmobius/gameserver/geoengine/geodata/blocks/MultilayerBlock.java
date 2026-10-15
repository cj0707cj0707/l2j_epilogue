package org.l2jmobius.gameserver.geoengine.geodata.blocks;

import java.nio.ByteBuffer;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;

public class MultilayerBlock implements IBlock {
   private final byte[] _data;

   public MultilayerBlock(ByteBuffer bb) {
      int start = bb.position();

      for (int blockCellOffset = 0; blockCellOffset < 64; blockCellOffset++) {
         byte nLayers = bb.get();
         if (nLayers <= 0 || nLayers > 125) {
            throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count!");
         }

         bb.position(bb.position() + nLayers * 2);
      }

      this._data = new byte[bb.position() - start];
      bb.position(start);
      bb.get(this._data);
   }

   private short getNearestLayer(int geoX, int geoY, int worldZ) {
      int startOffset = this.getCellDataOffset(geoX, geoY);
      byte nLayers = this._data[startOffset];
      int endOffset = startOffset + 1 + nLayers * 2;
      int nearestDZ = 0;
      short nearestData = 0;

      for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
         short layerData = this.extractLayerData(offset);
         int layerZ = this.extractLayerHeight(layerData);
         if (layerZ == worldZ) {
            return layerData;
         }

         int layerDZ = Math.abs(layerZ - worldZ);
         if (offset == startOffset + 1 || layerDZ < nearestDZ) {
            nearestDZ = layerDZ;
            nearestData = layerData;
         }
      }

      return nearestData;
   }

   private int getCellDataOffset(int geoX, int geoY) {
      int cellLocalOffset = geoX % 8 * 8 + geoY % 8;
      int cellDataOffset = 0;

      for (int i = 0; i < cellLocalOffset; i++) {
         cellDataOffset += 1 + this._data[cellDataOffset] * 2;
      }

      return cellDataOffset;
   }

   private short extractLayerData(int dataOffset) {
      return (short)(this._data[dataOffset] & 255 | this._data[dataOffset + 1] << 8);
   }

   private int getNearestNSWE(int geoX, int geoY, int worldZ) {
      return this.extractLayerNswe(this.getNearestLayer(geoX, geoY, worldZ));
   }

   @Override
   public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
      return (this.getNearestNSWE(geoX, geoY, worldZ) & nswe) == nswe;
   }

   @Override
   public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      int startOffset = this.getCellDataOffset(geoX, geoY);
      byte nLayers = this._data[startOffset];
      int endOffset = startOffset + 1 + nLayers * 2;
      int nearestDZ = 0;
      int nearestLayerZ = 0;
      int nearestOffset = 0;

      for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
         short layerData = this.extractLayerData(offset);
         int layerZ = this.extractLayerHeight(layerData);
         if (layerZ == worldZ) {
            nearestLayerZ = layerZ;
            nearestOffset = offset;
            break;
         }

         int layerDZ = Math.abs(layerZ - worldZ);
         if (offset == startOffset + 1 || layerDZ < nearestDZ) {
            nearestDZ = layerDZ;
            nearestLayerZ = layerZ;
            nearestOffset = offset;
         }
      }

      short currentNswe = this.getNearestNswe(geoX, geoY, worldZ);
      if ((currentNswe & nswe) == 0) {
         short encodedHeight = (short)(nearestLayerZ << 1);
         short newNswe = (short)(currentNswe | nswe);
         short newCombinedData = (short)(encodedHeight | newNswe);
         this._data[nearestOffset] = (byte)(newCombinedData & 255);
         this._data[nearestOffset + 1] = (byte)(newCombinedData >> 8 & 0xFF);
      }
   }

   @Override
   public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      int startOffset = this.getCellDataOffset(geoX, geoY);
      byte nLayers = this._data[startOffset];
      int endOffset = startOffset + 1 + nLayers * 2;
      int nearestDZ = 0;
      int nearestLayerZ = 0;
      int nearestOffset = 0;

      for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
         short layerData = this.extractLayerData(offset);
         int layerZ = this.extractLayerHeight(layerData);
         if (layerZ == worldZ) {
            nearestLayerZ = layerZ;
            nearestOffset = offset;
            break;
         }

         int layerDZ = Math.abs(layerZ - worldZ);
         if (offset == startOffset + 1 || layerDZ < nearestDZ) {
            nearestDZ = layerDZ;
            nearestLayerZ = layerZ;
            nearestOffset = offset;
         }
      }

      short currentNswe = this.getNearestNswe(geoX, geoY, worldZ);
      if ((currentNswe & nswe) != 0) {
         short encodedHeight = (short)(nearestLayerZ << 1);
         short newNswe = (short)(currentNswe & ~nswe);
         short newCombinedData = (short)(encodedHeight | newNswe);
         this._data[nearestOffset] = (byte)(newCombinedData & 255);
         this._data[nearestOffset + 1] = (byte)(newCombinedData >> 8 & 0xFF);
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

   private int extractLayerNswe(short layer) {
      return (byte)(layer & 15);
   }

   private int extractLayerHeight(short layer) {
      return (short)(layer & 65520) >> 1;
   }

   @Override
   public int getNearestZ(int geoX, int geoY, int worldZ) {
      int nearestZ = this.extractLayerHeight(this.getNearestLayer(geoX, geoY, worldZ));
      return nearestZ - worldZ > 1000 ? this.getNextLowerZ(geoX, geoY, worldZ) : nearestZ;
   }

   @Override
   public int getNextLowerZ(int geoX, int geoY, int worldZ) {
      int startOffset = this.getCellDataOffset(geoX, geoY);
      byte nLayers = this._data[startOffset];
      int endOffset = startOffset + 1 + nLayers * 2;
      int lowerZ = Integer.MIN_VALUE;

      for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
         short layerData = this.extractLayerData(offset);
         int layerZ = this.extractLayerHeight(layerData);
         if (layerZ == worldZ) {
            return layerZ;
         }

         if (layerZ < worldZ && layerZ > lowerZ) {
            lowerZ = layerZ;
         }
      }

      return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
   }

   @Override
   public int getNextHigherZ(int geoX, int geoY, int worldZ) {
      int startOffset = this.getCellDataOffset(geoX, geoY);
      byte nLayers = this._data[startOffset];
      int endOffset = startOffset + 1 + nLayers * 2;
      int higherZ = Integer.MAX_VALUE;

      for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
         short layerData = this.extractLayerData(offset);
         int layerZ = this.extractLayerHeight(layerData);
         if (layerZ == worldZ) {
            return layerZ;
         }

         if (layerZ > worldZ && layerZ < higherZ) {
            higherZ = layerZ;
         }
      }

      return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
   }

   public byte[] getData() {
      return this._data;
   }
}
