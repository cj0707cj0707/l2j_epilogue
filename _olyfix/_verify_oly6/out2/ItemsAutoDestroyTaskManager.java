package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;

public class ItemsAutoDestroyTaskManager implements Runnable {
   private static final Set<Item> ITEMS = ConcurrentHashMap.newKeySet();

   protected ItemsAutoDestroyTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 5000L, 5000L);
   }

   @Override
   public void run() {
      if (!ITEMS.isEmpty()) {
         long currentTime = System.currentTimeMillis();
         Iterator<Item> iterator = ITEMS.iterator();

         while (iterator.hasNext()) {
            Item itemInstance = iterator.next();
            if (itemInstance.getDropTime() != 0L && itemInstance.getItemLocation() == ItemLocation.VOID) {
               long autoDestroyTime;
               if (itemInstance.getTemplate().getAutoDestroyTime() > 0) {
                  autoDestroyTime = itemInstance.getTemplate().getAutoDestroyTime();
               } else if (itemInstance.getTemplate().hasExImmediateEffect()) {
                  autoDestroyTime = GeneralConfig.HERB_AUTO_DESTROY_TIME;
               } else {
                  autoDestroyTime = GeneralConfig.AUTODESTROY_ITEM_AFTER == 0 ? 3600000 : GeneralConfig.AUTODESTROY_ITEM_AFTER * 1000;
               }

               if (currentTime - itemInstance.getDropTime() > autoDestroyTime) {
                  itemInstance.decayMe();
                  if (GeneralConfig.SAVE_DROPPED_ITEM) {
                     ItemsOnGroundManager.getInstance().removeObject(itemInstance);
                  }

                  iterator.remove();
               }
            } else {
               iterator.remove();
            }
         }
      }
   }

   public void addItem(Item item) {
      item.setDropTime(System.currentTimeMillis());
      ITEMS.add(item);
   }

   public static org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager();
   }
}
