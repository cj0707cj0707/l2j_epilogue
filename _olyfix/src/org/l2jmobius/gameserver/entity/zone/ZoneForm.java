package org.l2jmobius.gameserver.entity.zone;

import java.awt.geom.Line2D;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ZoneManager;

public abstract class ZoneForm {
   protected static final int STEP = 10;

   public abstract boolean isInsideZone(int var1, int var2, int var3);

   public abstract boolean intersectsRectangle(int var1, int var2, int var3, int var4);

   public abstract double getDistanceToZone(int var1, int var2);

   public abstract int getLowZ();

   public abstract int getHighZ();

   protected boolean lineSegmentsIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
      return Line2D.linesIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2);
   }

   public abstract void visualizeZone(int var1);

   protected void dropDebugItem(int itemId, int num, int x, int y, int z) {
      Item item = new Item(IdManager.getInstance().getNextId(), itemId);
      item.setCount(num);
      item.spawnMe(x, y, z + 5);
      ZoneManager.getInstance().getDebugItems().add(item);
   }

   public abstract Location getRandomPoint();

   public abstract Location getCenterPoint();
}
