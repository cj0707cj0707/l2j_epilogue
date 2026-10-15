package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;

public class EnchantScrollGroup {
   private final int _id;
   private List<EnchantRateItem> _rateGroups;

   public EnchantScrollGroup(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }

   public void addRateGroup(EnchantRateItem group) {
      if (this._rateGroups == null) {
         this._rateGroups = new ArrayList<>();
      }

      this._rateGroups.add(group);
   }

   public List<EnchantRateItem> getRateGroups() {
      return this._rateGroups != null ? this._rateGroups : Collections.emptyList();
   }

   public EnchantRateItem getRateGroup(ItemTemplate item) {
      for (EnchantRateItem group : this.getRateGroups()) {
         if (group.validate(item)) {
            return group;
         }
      }

      return null;
   }
}
