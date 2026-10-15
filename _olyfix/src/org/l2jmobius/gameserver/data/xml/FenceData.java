package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.entity.actor.instance.Fence;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class FenceData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(FenceData.class.getSimpleName());
   private static final int MAX_Z_DIFF = 100;
   private final Map<Integer, Fence> _fences = new ConcurrentHashMap<>();

   protected FenceData() {
      this.load();
   }

   @Override
   public void load() {
      if (!this._fences.isEmpty()) {
         this._fences.values().forEach(this::removeFence);
      }

      this.parseDatapackFile("data/FenceData.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._fences.size() + " fences.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "fence", this::spawnFence));
   }

   public int getLoadedElementsCount() {
      return this._fences.size();
   }

   private void spawnFence(Node fenceNode) {
      StatSet set = new StatSet(this.parseAttributes(fenceNode));
      this.spawnFence(
         set.getInt("x"),
         set.getInt("y"),
         set.getInt("z"),
         set.getString("name"),
         set.getInt("width"),
         set.getInt("length"),
         set.getInt("height"),
         0,
         set.getEnum("state", FenceState.class, FenceState.CLOSED)
      );
   }

   public Fence spawnFence(int x, int y, int z, int width, int length, int height, int instanceId, FenceState state) {
      return this.spawnFence(x, y, z, null, width, length, height, instanceId, state);
   }

   public Fence spawnFence(int x, int y, int z, String name, int width, int length, int height, int instanceId, FenceState state) {
      Fence fence = new Fence(x, y, name, width, length, height, state);
      if (instanceId > 0) {
         fence.setInstanceId(instanceId);
      }

      fence.spawnMe(x, y, z);
      this.addFence(fence);
      return fence;
   }

   private void addFence(Fence fence) {
      this._fences.put(fence.getObjectId(), fence);
   }

   public void removeFence(Fence fence) {
      this._fences.remove(fence.getObjectId());
   }

   public Map<Integer, Fence> getFences() {
      return this._fences;
   }

   public Fence getFence(int objectId) {
      return this._fences.get(objectId);
   }

   public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId) {
      WorldRegion region = World.getRegion(x, y, z);
      Collection<Fence> fences = region != null ? region.getFences() : null;
      if (fences != null && !fences.isEmpty()) {
         for (Fence fence : fences) {
            if (fence.getState().isGeodataEnabled() && fence.getInstanceId() == instanceId) {
               int xMin = fence.getXMin();
               if (x >= xMin || tx >= xMin) {
                  int xMax = fence.getXMax();
                  if (x <= xMax || tx <= xMax) {
                     int yMin = fence.getYMin();
                     if (y >= yMin || ty >= yMin) {
                        int yMax = fence.getYMax();
                        if ((y <= yMax || ty <= yMax)
                           && (x <= xMin || tx <= xMin || x >= xMax || tx >= xMax || y <= yMin || ty <= yMin || y >= yMax || ty >= yMax)
                           && (
                              this.crossLinePart(xMin, yMin, xMax, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax)
                                 || this.crossLinePart(xMax, yMin, xMax, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax)
                                 || this.crossLinePart(xMax, yMax, xMin, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax)
                                 || this.crossLinePart(xMin, yMax, xMin, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax)
                           )
                           && z > fence.getZ() - 100
                           && z < fence.getZ() + 100) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean crossLinePart(
      double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double xMin, double yMin, double xMax, double yMax
   ) {
      double[] result = this.intersection(x1, y1, x2, y2, x3, y3, x4, y4);
      if (result == null) {
         return false;
      } else {
         double xCross = result[0];
         if (xCross <= xMax && xCross >= xMin) {
            return true;
         } else {
            double yCross = result[1];
            return yCross <= yMax && yCross >= yMin;
         }
      }
   }

   private double[] intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
      double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
      if (d == 0.0) {
         return null;
      } else {
         double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
         double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
         return new double[]{xi, yi};
      }
   }

   public static FenceData getInstance() {
      return FenceData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FenceData INSTANCE = new FenceData();
   }
}
