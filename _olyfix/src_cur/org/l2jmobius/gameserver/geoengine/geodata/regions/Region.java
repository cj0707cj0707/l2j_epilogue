package org.l2jmobius.gameserver.geoengine.geodata.regions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.ComplexBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.FlatBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.MultilayerBlock;

public class Region implements IRegion {
   private final IBlock[] _blocks = new IBlock[65536];

   public Region(ByteBuffer bb) {
      this.load(bb);
   }

   public void load(ByteBuffer bb) {
      for (int blockOffset = 0; blockOffset < 65536; blockOffset++) {
         int blockType = bb.get();
         switch (blockType) {
            case 0:
               this._blocks[blockOffset] = new FlatBlock(bb);
               break;
            case 1:
               this._blocks[blockOffset] = new ComplexBlock(bb);
               break;
            case 2:
               this._blocks[blockOffset] = new MultilayerBlock(bb);
               break;
            default:
               throw new RuntimeException("Invalid block type " + blockType + "!");
         }
      }
   }

   private IBlock getBlock(int geoX, int geoY) {
      return this._blocks[geoX / 8 % 256 * 256 + geoY / 8 % 256];
   }

   @Override
   public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
      return this.getBlock(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
   }

   @Override
   public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      IBlock block = this.getBlock(geoX, geoY);
      if (!(block instanceof FlatBlock)) {
         this.getBlock(geoX, geoY).setNearestNswe(geoX, geoY, worldZ, nswe);
      }
   }

   @Override
   public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
      IBlock block = this.getBlock(geoX, geoY);
      if (block instanceof FlatBlock) {
         this.convertFlatToComplex(block, geoX, geoY);
      }

      this.getBlock(geoX, geoY).unsetNearestNswe(geoX, geoY, worldZ, nswe);
   }

   private void convertFlatToComplex(IBlock block, int geoX, int geoY) {
      short currentHeight = ((FlatBlock)block).getHeight();
      short encodedHeight = (short)(currentHeight << 1 & 65535);
      short combinedData = (short)(encodedHeight | 15);
      ByteBuffer buffer = ByteBuffer.allocate(128);

      for (int i = 0; i < 64; i++) {
         buffer.putShort(combinedData);
      }

      buffer.rewind();
      this._blocks[geoX / 8 % 256 * 256 + geoY / 8 % 256] = new ComplexBlock(buffer);
   }

   @Override
   public int getNearestZ(int geoX, int geoY, int worldZ) {
      return this.getBlock(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
   }

   @Override
   public int getNextLowerZ(int geoX, int geoY, int worldZ) {
      return this.getBlock(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
   }

   @Override
   public int getNextHigherZ(int geoX, int geoY, int worldZ) {
      return this.getBlock(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
   }

   @Override
   public boolean hasGeo() {
      return true;
   }

   @Override
   public boolean saveToFile(String fileName) {
      Path filePath = new File(GeoEngineConfig.GEOEDIT_PATH + File.separator + fileName).toPath();
      if (Files.exists(filePath)) {
         try {
            Files.delete(filePath);
         } catch (IOException var15) {
            return false;
         }
      }

      try {
         try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            for (IBlock block : this._blocks) {
               if (block instanceof FlatBlock) {
                  ByteBuffer buffer = ByteBuffer.allocate(3);
                  buffer.put((byte)0);
                  buffer.putShort(Short.reverseBytes(((FlatBlock)block).getHeight()));
                  bos.write(buffer.array());
               } else if (block instanceof ComplexBlock) {
                  short[] data = ((ComplexBlock)block).getData();
                  ByteBuffer buffer = ByteBuffer.allocate(1 + data.length * 2);
                  buffer.put((byte)1);

                  for (short info : data) {
                     buffer.putShort(Short.reverseBytes(info));
                  }

                  bos.write(buffer.array());
               } else if (block instanceof MultilayerBlock) {
                  byte[] data = ((MultilayerBlock)block).getData();
                  ByteBuffer buffer = ByteBuffer.allocate(1 + data.length);
                  buffer.put((byte)2);

                  for (byte info : data) {
                     buffer.put(info);
                  }

                  bos.write(buffer.array());
               }
            }
         }

         return true;
      } catch (IOException var17) {
         return false;
      }
   }
}
