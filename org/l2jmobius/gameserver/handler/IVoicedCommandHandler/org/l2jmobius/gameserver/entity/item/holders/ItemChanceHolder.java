package org.l2jmobius.gameserver.entity.item.holders;

public class ItemChanceHolder extends ItemHolder {
   private final double _chance;

   public ItemChanceHolder(int id, double chance) {
      this(id, chance, 1L);
   }

   public ItemChanceHolder(int id, double chance, long count) {
      super(id, count);
      this._chance = chance;
   }

   public double getChance() {
      return this._chance;
   }

   @Override
   public String toString() {
      return "[" + this.getClass().getSimpleName() + "] ID: " + this.getId() + ", count: " + this.getCount() + ", chance: " + this._chance;
   }
}
