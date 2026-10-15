package org.l2jmobius.gameserver.taskmanagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.mechanics.buylist.Product;

public class BuyListTaskManager {
   protected static final Map<Product, Long> PRODUCTS = new ConcurrentHashMap<>();
   protected static final List<Product> PENDING_UPDATES = new ArrayList<>();
   protected static boolean _workingProducts = false;
   protected static boolean _workingSaves = false;

   protected BuyListTaskManager() {
      ThreadPool.scheduleAtFixedRate(new BuyListTaskManager.BuyListProductTask(), 1000L, 60000L);
      ThreadPool.scheduleAtFixedRate(new BuyListTaskManager.BuyListSaveTask(), 50L, 50L);
   }

   public void add(Product product, long endTime) {
      PRODUCTS.putIfAbsent(product, endTime);
   }

   public void update(Product product, long endTime) {
      PRODUCTS.put(product, endTime);
   }

   public long getRestockDelay(Product product) {
      return PRODUCTS.getOrDefault(product, 0L);
   }

   public static BuyListTaskManager getInstance() {
      return BuyListTaskManager.SingletonHolder.INSTANCE;
   }

   protected class BuyListProductTask implements Runnable {
      protected BuyListProductTask() {
         Objects.requireNonNull(BuyListTaskManager.this);
         super();
      }

      @Override
      public void run() {
         if (!BuyListTaskManager._workingProducts) {
            BuyListTaskManager._workingProducts = true;
            long currentTime = System.currentTimeMillis();

            for (Entry<Product, Long> entry : BuyListTaskManager.PRODUCTS.entrySet()) {
               if (currentTime > entry.getValue()) {
                  Product product = entry.getKey();
                  BuyListTaskManager.PRODUCTS.remove(product);
                  synchronized (BuyListTaskManager.PENDING_UPDATES) {
                     if (!BuyListTaskManager.PENDING_UPDATES.contains(product)) {
                        BuyListTaskManager.PENDING_UPDATES.add(product);
                     }
                  }
               }
            }

            BuyListTaskManager._workingProducts = false;
         }
      }
   }

   protected class BuyListSaveTask implements Runnable {
      protected BuyListSaveTask() {
         Objects.requireNonNull(BuyListTaskManager.this);
         super();
      }

      @Override
      public void run() {
         if (!BuyListTaskManager._workingSaves) {
            BuyListTaskManager._workingSaves = true;
            if (!BuyListTaskManager.PENDING_UPDATES.isEmpty()) {
               Product product;
               synchronized (BuyListTaskManager.PENDING_UPDATES) {
                  product = BuyListTaskManager.PENDING_UPDATES.get(0);
                  BuyListTaskManager.PENDING_UPDATES.remove(product);
               }

               product.restock();
            }

            BuyListTaskManager._workingSaves = false;
         }
      }
   }

   private static class SingletonHolder {
      protected static final BuyListTaskManager INSTANCE = new BuyListTaskManager();
   }
}
