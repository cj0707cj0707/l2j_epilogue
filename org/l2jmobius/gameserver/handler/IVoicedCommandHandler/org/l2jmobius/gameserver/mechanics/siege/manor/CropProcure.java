package org.l2jmobius.gameserver.mechanics.siege.manor;

public class CropProcure extends SeedProduction {
   private final int _rewardType;

   public CropProcure(int id, long amount, int type, long startAmount, long price) {
      super(id, amount, price, startAmount);
      this._rewardType = type;
   }

   public int getReward() {
      return this._rewardType;
   }
}
