package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

public class UCReward {
   private final int _itemId;
   private final long _amount;
   private final boolean _allowMidifier;

   public UCReward(int itemId, long amount, boolean allowMidifier) {
      this._itemId = itemId;
      this._amount = amount;
      this._allowMidifier = allowMidifier;
   }

   public int getId() {
      return this._itemId;
   }

   public long getAmount() {
      return this._amount;
   }

   public boolean isAllowMidifier() {
      return this._allowMidifier;
   }
}
