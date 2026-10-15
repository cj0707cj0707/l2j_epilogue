package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.data.holders.RangeChanceHolder;

public class EnchantItemGroup {
   private static final Logger LOGGER = Logger.getLogger(EnchantItemGroup.class.getName());
   private final List<RangeChanceHolder> _chances = new ArrayList<>();
   private final String _name;

   public EnchantItemGroup(String name) {
      this._name = name;
   }

   public String getName() {
      return this._name;
   }

   public void addChance(RangeChanceHolder holder) {
      this._chances.add(holder);
   }

   public double getChance(int index) {
      if (!this._chances.isEmpty()) {
         for (RangeChanceHolder holder : this._chances) {
            if (holder.getMin() <= index && holder.getMax() >= index) {
               return holder.getChance();
            }
         }

         return this._chances.get(this._chances.size() - 1).getChance();
      } else {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": item group: " + this._name + " doesn't have any chances!");
         return -1.0;
      }
   }
}
