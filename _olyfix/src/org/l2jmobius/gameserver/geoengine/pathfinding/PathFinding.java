package org.l2jmobius.gameserver.geoengine.pathfinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class PathFinding {
   private static final Logger LOGGER = Logger.getLogger(PathFinding.class.getName());
   private final int[] _sizeClasses;
   private final ThreadLocal<NodeBuffer[]> _tlBuffers;

   protected PathFinding() {
      try {
         String[] array = GeoEngineConfig.PATHFIND_BUFFERS.split(";");
         int[] parsed = new int[array.length];

         for (int i = 0; i < array.length; i++) {
            String[] args = array[i].split("x");
            if (args.length < 1) {
               throw new Exception("Invalid buffer definition: " + array[i]);
            }

            parsed[i] = Integer.parseInt(args[0]);
         }

         Arrays.sort(parsed);
         this._sizeClasses = parsed;
         this._tlBuffers = ThreadLocal.withInitial(() -> new NodeBuffer[this._sizeClasses.length]);
      } catch (Exception var5) {
         LOGGER.log(Level.WARNING, "CellPathFinding: Problem during buffer init: " + var5.getMessage(), (Throwable)var5);
         throw new Error("CellPathFinding: load aborted");
      }
   }

   public short getNodePos(int geoPos) {
      return (short)(geoPos >> 3);
   }

   public short getNodeBlock(int nodePos) {
      return (short)(nodePos % 256);
   }

   public byte getRegionX(int nodePos) {
      return (byte)((nodePos >> 8) + 11);
   }

   public byte getRegionY(int nodePos) {
      return (byte)((nodePos >> 8) + 10);
   }

   public short getRegionOffset(byte rx, byte ry) {
      return (short)((rx << 5) + ry);
   }

   public int calculateWorldX(short nodeX) {
      return -294912 + nodeX * 128 + 48;
   }

   public int calculateWorldY(short nodeY) {
      return -262144 + nodeY * 128 + 48;
   }

   public List<GeoLocation> findPath(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean playable) {
      GeoEngine geoEngine = GeoEngine.getInstance();
      if (!geoEngine.hasGeo(x, y)) {
         return null;
      } else if (!geoEngine.hasGeo(tx, ty)) {
         return null;
      } else {
         int gx = GeoEngine.getGeoX(x);
         int gy = GeoEngine.getGeoY(y);
         int gtx = GeoEngine.getGeoX(tx);
         int gty = GeoEngine.getGeoY(ty);
         NodeBuffer buffer = this.alloc(64 + 2 * Math.max(Math.abs(gx - gtx), Math.abs(gy - gty)));
         if (buffer == null) {
            return null;
         } else {
            int gz = geoEngine.getHeight(x, y, z);
            int gtz = geoEngine.getHeight(tx, ty, tz);

            List<GeoLocation> path;
            label59: {
               Object var19;
               try {
                  path = buffer.findPath(gx, gy, gz, gtx, gty, gtz);
                  break label59;
               } catch (Exception var23) {
                  LOGGER.log(Level.WARNING, "CellPathFinding: Problem finding path: " + var23.getMessage(), (Throwable)var23);
                  var19 = null;
               } finally {
                  buffer.free();
               }

               return (List<GeoLocation>)var19;
            }

            if (path == null) {
               return null;
            } else {
               return path.size() >= 3 && GeoEngineConfig.MAX_POSTFILTER_PASSES > 0 ? this.applyPostFiltering(path, x, y, z, instanceId, playable) : path;
            }
         }
      }
   }

   private List<GeoLocation> applyPostFiltering(List<GeoLocation> initialPath, int startX, int startY, int startZ, int instanceId, boolean playable) {
      GeoEngine geoEngine = GeoEngine.getInstance();
      List<GeoLocation> path = initialPath;
      int pass = 0;

      boolean changed;
      List<GeoLocation> optimizedPath;
      do {
         pass++;
         changed = false;
         int currentX = startX;
         int currentY = startY;
         int currentZ = startZ;
         optimizedPath = new ArrayList<>();

         for (int i = 0; i < path.size() - 1; i++) {
            GeoLocation current = path.get(i);
            GeoLocation next = path.get(i + 1);
            if (geoEngine.canMoveToTarget(currentX, currentY, currentZ, next.getX(), next.getY(), next.getZ(), instanceId)) {
               changed = true;
            } else {
               optimizedPath.add(current);
               currentX = current.getX();
               currentY = current.getY();
               currentZ = current.getZ();
            }
         }

         if (!path.isEmpty()) {
            optimizedPath.add(path.get(path.size() - 1));
         }

         path = optimizedPath;
      } while (playable && changed && optimizedPath.size() > 2 && pass < GeoEngineConfig.MAX_POSTFILTER_PASSES);

      return optimizedPath;
   }

   private NodeBuffer alloc(int size) {
      NodeBuffer[] perThread = this._tlBuffers.get();

      for (int i = 0; i < this._sizeClasses.length; i++) {
         if (this._sizeClasses[i] >= size) {
            NodeBuffer buffer = perThread[i];
            if (buffer == null) {
               buffer = new NodeBuffer(this._sizeClasses[i]);
               perThread[i] = buffer;
            }

            return buffer;
         }
      }

      return null;
   }

   public static PathFinding getInstance() {
      return PathFinding.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PathFinding INSTANCE = new PathFinding();
   }
}
