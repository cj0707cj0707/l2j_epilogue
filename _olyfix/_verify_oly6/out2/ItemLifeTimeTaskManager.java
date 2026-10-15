package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ItemLifeTimeTaskManager implements Runnable {
   private static final Map<Item, Long> ITEMS = new ConcurrentHashMap<>();
   private static boolean _working = false;

   protected ItemLifeTimeTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 1000L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!ITEMS.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<Item, Long>> iterator = ITEMS.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Item, Long> entry = iterator.next();
               if (currentTime > entry.getValue()) {
                  entry.getKey().endOfLife();
                  iterator.remove();
               }
            }
         }

         _working = false;
      }
   }

   public void add(Item item, long endTime) {
      ITEMS.putIfAbsent(item, endTime);
   }

   public void remove(Item item) {
      ITEMS.remove(item);
   }

   public static org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager();
   }
}
