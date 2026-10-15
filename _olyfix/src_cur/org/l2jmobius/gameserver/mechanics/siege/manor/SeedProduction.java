package org.l2jmobius.gameserver.mechanics.siege.manor;

import java.util.concurrent.atomic.AtomicLong;

public class SeedProduction {
   private final int _seedId;
   private final long _price;
   private final long _startAmount;
   private final AtomicLong _amount;

   public SeedProduction(int id, long amount, long price, long startAmount) {
      this._seedId = id;
      this._amount = new AtomicLong(amount);
      this._price = price;
      this._startAmount = startAmount;
   }

   public int getId() {
      return this._seedId;
   }

   public long getAmount() {
      return this._amount.get();
   }

   public long getPrice() {
      return this._price;
   }

   public long getStartAmount() {
      return this._startAmount;
   }

   public void setAmount(long amount) {
      this._amount.set(amount);
   }

   public boolean decreaseAmount(long value) {
      long current;
      long next;
      do {
         current = this._amount.get();
         next = current - value;
         if (next < 0L) {
            return false;
         }
      } while (!this._amount.compareAndSet(current, next));

      return true;
   }
}
