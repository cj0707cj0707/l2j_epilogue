package org.l2jmobius.gameserver.mechanics.buylist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.taskmanagers.BuyListTaskManager;

public class Product {
   private static final Logger LOGGER = Logger.getLogger(Product.class.getName());
   private final int _buyListId;
   private final ItemTemplate _item;
   private final long _price;
   private final long _restockDelay;
   private final long _maxCount;
   private AtomicLong _count = null;

   public Product(int buyListId, ItemTemplate item, long price, long restockDelay, long maxCount) {
      this._buyListId = buyListId;
      this._item = item;
      this._price = price;
      this._restockDelay = restockDelay * 60000L;
      this._maxCount = maxCount;
      if (this.hasLimitedStock()) {
         this._count = new AtomicLong(maxCount);
      }
   }

   public int getBuyListId() {
      return this._buyListId;
   }

   public ItemTemplate getItem() {
      return this._item;
   }

   public int getItemId() {
      return this._item.getId();
   }

   public long getPrice() {
      return this._price < 0L ? this._item.getReferencePrice() : this._price;
   }

   public long getRestockDelay() {
      return this._restockDelay;
   }

   public long getMaxCount() {
      return this._maxCount;
   }

   public long getCount() {
      if (this._count == null) {
         return 0L;
      } else {
         long count = this._count.get();
         return count > 0L ? count : 0L;
      }
   }

   public void setCount(long currentCount) {
      if (this._count == null) {
         this._count = new AtomicLong();
      }

      this._count.set(currentCount);
   }

   public boolean decreaseCount(long value) {
      if (this._count == null) {
         return false;
      } else {
         BuyListTaskManager.getInstance().add(this, System.currentTimeMillis() + this._restockDelay);
         boolean result = this._count.addAndGet(-value) >= 0L;
         this.save();
         return result;
      }
   }

   public boolean hasLimitedStock() {
      return this._maxCount > -1L;
   }

   public void restartRestockTask(long nextRestockTime) {
      long remainTime = nextRestockTime - System.currentTimeMillis();
      if (remainTime > 0L) {
         BuyListTaskManager.getInstance().update(this, remainTime);
      } else {
         this.restock();
      }
   }

   public void restock() {
      this.setCount(this._maxCount);
      this.save();
   }

   private void save() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "INSERT INTO `buylists`(`buylist_id`, `item_id`, `count`, `next_restock_time`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `count` = ?, `next_restock_time` = ?"
         );
      ) {
         statement.setInt(1, this._buyListId);
         statement.setInt(2, this._item.getId());
         statement.setLong(3, this.getCount());
         statement.setLong(5, this.getCount());
         long nextRestockTime = BuyListTaskManager.getInstance().getRestockDelay(this);
         if (nextRestockTime > 0L) {
            statement.setLong(4, nextRestockTime);
            statement.setLong(6, nextRestockTime);
         } else {
            statement.setLong(4, 0L);
            statement.setLong(6, 0L);
         }

         statement.executeUpdate();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Failed to save Product buylist_id:" + this._buyListId + " item_id:" + this._item.getId(), (Throwable)var9);
      }
   }
}
