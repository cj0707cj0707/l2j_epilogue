package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ItemManaTaskManager implements Runnable {
   private static final Map<Item, Long> ITEMS = new ConcurrentHashMap<>();
   private static final int MANA_CONSUMPTION_RATE = 60000;
   private static boolean _working = false;

   protected ItemManaTaskManager() {
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
                  iterator.remove();
                  Item item = entry.getKey();
                  Player player = item.asPlayer();
                  if (player != null && !player.isInOfflineMode()) {
                     item.decreaseMana(item.isEquipped());
                  }
               }
            }
         }

         _working = false;
      }
   }

   public void add(Item item) {
      ITEMS.computeIfAbsent(item, k -> System.currentTimeMillis() + 60000L);
   }

   public static org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager();
   }
}
