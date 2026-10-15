package org.l2jmobius.gameserver.entity.item.holders;

public class ItemEnchantHolder extends ItemHolder {
   private final int _enchantLevel;

   public ItemEnchantHolder(int id, long count) {
      super(id, count);
      this._enchantLevel = 0;
   }

   public ItemEnchantHolder(int id, long count, int enchantLevel) {
      super(id, count);
      this._enchantLevel = enchantLevel;
   }

   public int getEnchantLevel() {
      return this._enchantLevel;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof ItemEnchantHolder objInstance) {
         return obj == this
            ? true
            : this.getId() == objInstance.getId() && this.getCount() == objInstance.getCount() && this._enchantLevel == objInstance.getEnchantLevel();
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "[" + this.getClass().getSimpleName() + "] ID: " + this.getId() + ", count: " + this.getCount() + ", enchant level: " + this._enchantLevel;
   }
}
