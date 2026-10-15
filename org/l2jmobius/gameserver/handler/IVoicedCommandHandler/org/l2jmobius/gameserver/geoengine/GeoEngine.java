package org.l2jmobius.gameserver.geoengine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.NullRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.Region;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.util.GeoUtils;

public class GeoEngine {
   private static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
   public static final String FILE_NAME_FORMAT = "%d_%d.l2j";
   private static final int ELEVATED_SEE_OVER_DISTANCE = 2;
   private static final int MAX_SEE_OVER_HEIGHT = 48;
   private static final int SPAWN_Z_DELTA_LIMIT = 100;
   private static final int WORLD_MIN_X = -655360;
   private static final int WORLD_MIN_Y = -589824;
   private static final int GEO_REGIONS_X = 32;
   private static final int GEO_REGIONS_Y = 32;
   private static final int GEO_REGIONS = 1024;
   private static final int COORDINATE_SCALE = 16;
   private static final int COORDINATE_OFFSET = 8;
   private static final int HEIGHT_INCREASE_LIMIT = 40;
   private static final int SPAWN_HEIGHT_OFFSET = 20;
   private static final int PATH_CONTINUITY_TOLERANCE = 16;
   private static final AtomicReferenceArray<IRegion> REGIONS = new AtomicReferenceArray<>(1024);

   protected GeoEngine() {
      for (int var1 = 0; var1 < 1024; var1++) {
         REGIONS.set(var1, NullRegion.INSTANCE);
      }

      int var8 = 0;

      try {
         for (int var2 = 11; var2 <= 26; var2++) {
            for (int var3 = 10; var3 <= 26; var3++) {
               Path var4 = GeoEngineConfig.GEODATA_PATH.resolve(String.format("%d_%d.l2j", var2, var3));
               if (Files.exists(var4)) {
                  try {
                     this.loadRegion(var4, var2, var3);
                     var8++;
                  } catch (Exception var6) {
                     LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Failed to load " + var4.getFileName() + "!", (Throwable)var6);
                  }
               }
            }
         }
      } catch (Exception var7) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Critical error during geodata initialization!", (Throwable)var7);
         System.exit(1);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + var8 + " regions.");
      if (var8 == 0 && GeoEngineConfig.PATHFINDING > 0) {
         GeoEngineConfig.PATHFINDING = 0;
         LOGGER.info(this.getClass().getSimpleName() + ": Pathfinding is disabled.");
      }
   }

   private void loadRegion(Path var1, int var2, int var3) throws IOException {
      int var4 = var2 * 32 + var3;

      try (RandomAccessFile var5 = new RandomAccessFile(var1.toFile(), "r")) {
         REGIONS.set(var4, new Region(var5.getChannel().map(MapMode.READ_ONLY, 0L, var5.length()).order(ByteOrder.LITTLE_ENDIAN)));
      }
   }

   public boolean reloadRegion(int var1, int var2) {
      Path var3 = GeoEngineConfig.GEODATA_PATH.resolve(String.format("%d_%d.l2j", var1, var2));
      if (!Files.exists(var3)) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Cannot reload, file not found for region " + var1 + "_" + var2);
         return false;
      } else {
         int var4 = var1 * 32 + var2;

         try {
            IRegion var5 = REGIONS.get(var4);
            if (var5 instanceof Region) {
               ByteBuffer var6 = ByteBuffer.wrap(Files.readAllBytes(var3)).order(ByteOrder.LITTLE_ENDIAN);
               ((Region)var5).load(var6);
               LOGGER.info(this.getClass().getSimpleName() + ": Reloaded region " + var1 + "_" + var2 + " from bytes.");
               return true;
            } else {
               this.loadRegion(var3, var1, var2);
               LOGGER.info(this.getClass().getSimpleName() + ": Replaced NullRegion with new region " + var1 + "_" + var2);
               return true;
            }
         } catch (Exception var7) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Failed to reload region " + var1 + "_" + var2 + "!", (Throwable)var7);
            return false;
         }
      }
   }

   public void setRegion(int var1, int var2, Region var3) {
      int var4 = var1 * 32 + var2;
      REGIONS.set(var4, var3);
   }

   public IRegion getRegion(int var1, int var2) {
      return REGIONS.get(var1 / 2048 * 32 + var2 / 2048);
   }

   public boolean hasGeoPos(int var1, int var2) {
      return this.getRegion(var1, var2).hasGeo();
   }

   public boolean checkNearestNswe(int var1, int var2, int var3, int var4) {
      return this.getRegion(var1, var2).checkNearestNswe(var1, var2, var3, var4);
   }

   public boolean checkNearestNsweAntiCornerCut(int var1, int var2, int var3, int var4) {
      boolean var5 = true;
      IRegion var6 = this.getRegion(var1, var2);
      if ((var4 & 9) == 9) {
         var5 = var6.checkNearestNswe(var1, var2 - 1, var3, 1) && var6.checkNearestNswe(var1 + 1, var2, var3, 8);
      }

      if (var5 && (var4 & 10) == 10) {
         var5 = var6.checkNearestNswe(var1, var2 - 1, var3, 2) && var6.checkNearestNswe(var1 - 1, var2, var3, 8);
      }

      if (var5 && (var4 & 5) == 5) {
         var5 = var6.checkNearestNswe(var1, var2 + 1, var3, 1) && var6.checkNearestNswe(var1 + 1, var2, var3, 4);
      }

      if (var5 && (var4 & 6) == 6) {
         var5 = var6.checkNearestNswe(var1, var2 + 1, var3, 2) && var6.checkNearestNswe(var1 - 1, var2, var3, 4);
      }

      return var5 && var6.checkNearestNswe(var1, var2, var3, var4);
   }

   public void setNearestNswe(int var1, int var2, int var3, byte var4) {
      this.getRegion(var1, var2).setNearestNswe(var1, var2, var3, var4);
   }

   public void unsetNearestNswe(int var1, int var2, int var3, byte var4) {
      this.getRegion(var1, var2).unsetNearestNswe(var1, var2, var3, var4);
   }

   public int getNearestZ(int var1, int var2, int var3) {
      return this.getRegion(var1, var2).getNearestZ(var1, var2, var3);
   }

   public int getNextLowerZ(int var1, int var2, int var3) {
      return this.getRegion(var1, var2).getNextLowerZ(var1, var2, var3);
   }

   public int getNextHigherZ(int var1, int var2, int var3) {
      return this.getRegion(var1, var2).getNextHigherZ(var1, var2, var3);
   }

   public static int getGeoX(int var0) {
      return (var0 - -655360) / 16;
   }

   public static int getGeoY(int var0) {
      return (var0 - -589824) / 16;
   }

   public static int getWorldX(int var0) {
      return var0 * 16 + -655360 + 8;
   }

   public static int getWorldY(int var0) {
      return var0 * 16 + -589824 + 8;
   }

   public int getHeight(int var1, int var2, int var3) {
      return this.getNearestZ(getGeoX(var1), getGeoY(var2), var3);
   }

   public int getSpawnHeight(int var1, int var2, int var3) {
      int var4 = getGeoX(var1);
      int var5 = getGeoY(var2);
      if (!this.hasGeoPos(var4, var5)) {
         return var3;
      } else {
         int var6 = this.getNextLowerZ(var4, var5, var3 + 20);
         return Math.abs(var6 - var3) <= 100 ? var6 : var3;
      }
   }

   public int getSpawnHeight(Location var1) {
      return this.getSpawnHeight(var1.getX(), var1.getY(), var1.getZ());
   }

   public boolean canSeeTarget(WorldObject var1, WorldObject var2) {
      return var2 != null
         && (
            var2.isDoor()
               || var2.isArtefact()
               || this.canSeeTarget(var1.getX(), var1.getY(), var1.getZ(), var1.getInstanceId(), var2.getX(), var2.getY(), var2.getZ(), var2.getInstanceId())
         );
   }

   public boolean canSeeTarget(WorldObject var1, ILocational var2) {
      return this.canSeeTarget(var1.getX(), var1.getY(), var1.getZ(), var2.getX(), var2.getY(), var2.getZ(), var1.getInstanceId());
   }

   public boolean canSeeTarget(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      return var4 == var8 && this.canSeeTarget(var1, var2, var3, var5, var6, var7, var4);
   }

   public boolean canSeeTarget(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return true;
   }

   private int getLosGeoZ(int var1, int var2, int var3, int var4, int var5, int var6) {
      if (((var6 & 8) == 0 || (var6 & 4) == 0) && ((var6 & 2) == 0 || (var6 & 1) == 0)) {
         return this.checkNearestNsweAntiCornerCut(var1, var2, var3, var6) ? this.getNearestZ(var4, var5, var3) : this.getNextHigherZ(var4, var5, var3);
      } else {
         throw new RuntimeException("Multiple directions specified in NSWE: " + var6);
      }
   }

   public boolean canSeeTarget(int var1, int var2, int var3, int var4, int var5, int var6) {
      int var7 = getGeoX(var1);
      int var8 = getGeoY(var2);
      int var9 = getGeoX(var4);
      int var10 = getGeoY(var5);
      int var11 = this.getNearestZ(var7, var8, var3);
      int var12 = this.getNearestZ(var9, var10, var6);
      if (var7 == var9 && var8 == var10) {
         return !this.hasGeoPos(var9, var10) || var11 == var12;
      } else {
         if (var12 > var11) {
            int var13 = var12;
            var12 = var11;
            var11 = var13;
            var13 = var9;
            var9 = var7;
            var7 = var13;
            var13 = var10;
            var10 = var8;
            var8 = var13;
         }

         GridLineIterator3D var29 = new GridLineIterator3D(var7, var8, var11, var9, var10, var12);
         var29.next();
         int var14 = var29.x();
         int var15 = var29.y();
         int var16 = var29.z();
         int var17 = 0;

         while (var29.next()) {
            int var18 = var29.x();
            int var19 = var29.y();
            if (var18 != var14 || var19 != var15) {
               int var20 = var29.z();
               int var21 = var16;
               if (this.hasGeoPos(var18, var19)) {
                  int var22 = GeoUtils.computeNswe(var14, var15, var18, var19);
                  var21 = this.getLosGeoZ(var14, var15, var16, var18, var19, var22);
                  int var23 = var17 < 2 ? var11 + 48 : var20 + 48;
                  boolean var24 = false;
                  if (var21 <= var23) {
                     if ((var22 & 9) == 9) {
                        int var32 = this.getLosGeoZ(var14, var15, var16, var14, var15 - 1, 1);
                        int var35 = this.getLosGeoZ(var14, var15, var16, var14 + 1, var15, 8);
                        var24 = var32 <= var23
                           && var35 <= var23
                           && var32 <= this.getNearestZ(var14, var15 - 1, var20)
                           && var35 <= this.getNearestZ(var14 + 1, var15, var20);
                     } else if ((var22 & 10) == 10) {
                        int var31 = this.getLosGeoZ(var14, var15, var16, var14, var15 - 1, 2);
                        int var34 = this.getLosGeoZ(var14, var15, var16, var14 - 1, var15, 8);
                        var24 = var31 <= var23
                           && var34 <= var23
                           && var31 <= this.getNearestZ(var14, var15 - 1, var20)
                           && var34 <= this.getNearestZ(var14 - 1, var15, var20);
                     } else if ((var22 & 5) == 5) {
                        int var30 = this.getLosGeoZ(var14, var15, var16, var14, var15 + 1, 1);
                        int var33 = this.getLosGeoZ(var14, var15, var16, var14 + 1, var15, 4);
                        var24 = var30 <= var23
                           && var33 <= var23
                           && var30 <= this.getNearestZ(var14, var15 + 1, var20)
                           && var33 <= this.getNearestZ(var14 + 1, var15, var20);
                     } else if ((var22 & 6) != 6) {
                        var24 = true;
                     } else {
                        int var25 = this.getLosGeoZ(var14, var15, var16, var14, var15 + 1, 2);
                        int var26 = this.getLosGeoZ(var14, var15, var16, var14 - 1, var15, 4);
                        var24 = var25 <= var23
                           && var26 <= var23
                           && var25 <= this.getNearestZ(var14, var15 + 1, var20)
                           && var26 <= this.getNearestZ(var14 - 1, var15, var20);
                     }
                  }

                  if (!var24) {
                     return false;
                  }
               }

               var14 = var18;
               var15 = var19;
               var16 = var21;
               var17++;
            }
         }

         return true;
      }
   }

   private boolean hasNeighbourLayerNear(int var1, int var2, int var3, int var4) {
      int[] var5 = new int[]{1, -1, 0, 0};
      int[] var6 = new int[]{0, 0, 1, -1};

      for (int var7 = 0; var7 < 4; var7++) {
         int var8 = var1 + var5[var7];
         int var9 = var2 + var6[var7];
         if (this.hasGeoPos(var8, var9) && Math.abs(this.getNearestZ(var8, var9, var3) - var3) <= var4) {
            return true;
         }
      }

      return false;
   }

   public Location getValidLocation(ILocational var1, ILocational var2) {
      return this.getValidLocation(var1.getX(), var1.getY(), var1.getZ(), var2.getX(), var2.getY(), var2.getZ(), 0);
   }

   public Location getValidLocation(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = getGeoX(var1);
      int var9 = getGeoY(var2);
      int var10 = this.getNearestZ(var8, var9, var3);
      int var11 = getGeoX(var4);
      int var12 = getGeoY(var5);
      int var13 = this.getNearestZ(var11, var12, var6);
      if (DoorData.getInstance().checkIfDoorsBetween(var1, var2, var10, var4, var5, var13, var7, false)) {
         return new Location(var1, var2, this.getHeight(var1, var2, var10));
      } else if (FenceData.getInstance().checkIfFenceBetween(var1, var2, var10, var4, var5, var13, var7)) {
         return new Location(var1, var2, this.getHeight(var1, var2, var10));
      } else {
         GridLineIterator2D var14 = new GridLineIterator2D(var8, var9, var11, var12);
         var14.next();
         int var15 = var14.x();
         int var16 = var14.y();
         int var17 = var10;

         while (var14.next()) {
            int var18 = var14.x();
            int var19 = var14.y();
            int var20 = this.getNearestZ(var18, var19, var17);
            int var21;
            if (var20 - var17 > 40) {
               if (!this.hasNeighbourLayerNear(var18, var19, var17, 16)) {
                  return new Location(getWorldX(var15), getWorldY(var16), var17);
               }

               var21 = var17;
            } else {
               var21 = var20;
            }

            if (this.hasGeoPos(var15, var16)) {
               if (this.isCompletelyBlocked(var18, var19, var21)) {
                  return new Location(getWorldX(var15), getWorldY(var16), var17);
               }

               if (!this.checkNearestNsweAntiCornerCut(var15, var16, var17, GeoUtils.computeNswe(var15, var16, var18, var19))) {
                  return new Location(getWorldX(var15), getWorldY(var16), var17);
               }
            }

            var15 = var18;
            var16 = var19;
            var17 = var21;
         }

         return this.hasGeoPos(var15, var16) && var17 != var13 ? new Location(var1, var2, var10) : new Location(var4, var5, var13);
      }
   }

   public boolean canMoveToTarget(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return true;
   }

   public boolean canMoveToTarget(ILocational var1, int var2, int var3, int var4) {
      return this.canMoveToTarget(var1.getX(), var1.getY(), var1.getZ(), var2, var3, var4, 0);
   }

   public boolean canMoveToTarget(ILocational var1, ILocational var2) {
      return this.canMoveToTarget(var1, var2.getX(), var2.getY(), var2.getZ());
   }

   public boolean hasGeo(int var1, int var2) {
      return this.hasGeoPos(getGeoX(var1), getGeoY(var2));
   }

   public boolean isCompletelyBlocked(int var1, int var2, int var3) {
      if (GeoEngineConfig.PATHFINDING < 1) {
         return false;
      } else {
         IRegion var4 = this.getRegion(var1, var2);
         return var4 == null
            ? true
            : var4.hasGeo()
               && !var4.checkNearestNswe(var1, var2, var3, 8)
               && !var4.checkNearestNswe(var1, var2, var3, 4)
               && !var4.checkNearestNswe(var1, var2, var3, 1)
               && !var4.checkNearestNswe(var1, var2, var3, 2);
      }
   }

   public static GeoEngine getInstance() {
      return GeoEngine.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final GeoEngine INSTANCE = new GeoEngine();
   }
}
