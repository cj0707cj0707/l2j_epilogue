package org.l2jmobius.gameserver.mechanics.siege.manor;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.util.StatSet;

public class Seed {
   private final int _seedId;
   private final int _cropId;
   private final int _level;
   private final int _matureId;
   private final int _reward1;
   private final int _reward2;
   private final int _castleId;
   private final boolean _isAlternative;
   private final int _limitSeeds;
   private final int _limitCrops;
   private final int _seedReferencePrice;
   private final int _cropReferencePrice;

   public Seed(StatSet set) {
      this._cropId = set.getInt("id");
      this._seedId = set.getInt("seedId");
      this._level = set.getInt("level");
      this._matureId = set.getInt("mature_Id");
      this._reward1 = set.getInt("reward1");
      this._reward2 = set.getInt("reward2");
      this._castleId = set.getInt("castleId");
      this._isAlternative = set.getBoolean("alternative");
      this._limitCrops = set.getInt("limit_crops");
      this._limitSeeds = set.getInt("limit_seed");
      ItemTemplate item = ItemData.getInstance().getTemplate(this._cropId);
      this._cropReferencePrice = item != null ? item.getReferencePrice() : 1;
      item = ItemData.getInstance().getTemplate(this._seedId);
      this._seedReferencePrice = item != null ? item.getReferencePrice() : 1;
   }

   public int getCastleId() {
      return this._castleId;
   }

   public int getSeedId() {
      return this._seedId;
   }

   public int getCropId() {
      return this._cropId;
   }

   public int getMatureId() {
      return this._matureId;
   }

   public int getReward(int type) {
      return type == 1 ? this._reward1 : this._reward2;
   }

   public int getLevel() {
      return this._level;
   }

   public boolean isAlternative() {
      return this._isAlternative;
   }

   public int getSeedLimit() {
      return this._limitSeeds * RatesConfig.RATE_DROP_MANOR;
   }

   public int getCropLimit() {
      return this._limitCrops * RatesConfig.RATE_DROP_MANOR;
   }

   public int getSeedReferencePrice() {
      return this._seedReferencePrice;
   }

   public int getSeedMaxPrice() {
      return this._seedReferencePrice * 10;
   }

   public int getSeedMinPrice() {
      return (int)(this._seedReferencePrice * 0.6);
   }

   public int getCropReferencePrice() {
      return this._cropReferencePrice;
   }

   public int getCropMaxPrice() {
      return this._cropReferencePrice * 10;
   }

   public int getCropMinPrice() {
      return (int)(this._cropReferencePrice * 0.6);
   }

   @Override
   public String toString() {
      return "SeedData [_id="
         + this._seedId
         + ", _level="
         + this._level
         + ", _crop="
         + this._cropId
         + ", _mature="
         + this._matureId
         + ", _type1="
         + this._reward1
         + ", _type2="
         + this._reward2
         + ", _manorId="
         + this._castleId
         + ", _isAlternative="
         + this._isAlternative
         + ", _limitSeeds="
         + this._limitSeeds
         + ", _limitCrops="
         + this._limitCrops
         + "]";
   }
}
