package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.util.StatSet;

public class EnchantScroll extends AbstractEnchantItem {
   private final boolean _isWeapon;
   private final boolean _isBlessed;
   private final boolean _isSafe;
   private final int _scrollGroupId;
   private final Map<Integer, Integer> _items = new HashMap<>();

   public EnchantScroll(StatSet set) {
      super(set);
      this._scrollGroupId = set.getInt("scrollGroupId", 0);
      ItemType type = this.getItem().getItemType();
      this._isWeapon = type == EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP || type == EtcItemType.BLESS_SCRL_ENCHANT_WP || type == EtcItemType.SCRL_ENCHANT_WP;
      this._isBlessed = type == EtcItemType.BLESS_SCRL_ENCHANT_AM || type == EtcItemType.BLESS_SCRL_ENCHANT_WP;
      this._isSafe = type == EtcItemType.ANCIENT_CRYSTAL_ENCHANT_AM || type == EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP;
   }

   @Override
   public boolean isWeapon() {
      return this._isWeapon;
   }

   public boolean isBlessed() {
      return this._isBlessed;
   }

   public boolean isSafe() {
      return this._isSafe;
   }

   public void addItem(int itemId, int scrollGroupId) {
      this._items.put(itemId, scrollGroupId > -1 ? scrollGroupId : this._scrollGroupId);
   }

   public Collection<Integer> getItems() {
      return this._items.keySet();
   }

   @Override
   public boolean isValid(Item itemToEnchant, EnchantSupportItem supportItem) {
      if (!this._items.isEmpty() && !this._items.containsKey(itemToEnchant.getId())) {
         return false;
      } else {
         if (supportItem != null) {
            if (this._isBlessed) {
               return false;
            }

            if (!supportItem.isValid(itemToEnchant, supportItem)) {
               return false;
            }

            if (supportItem.isWeapon() != this.isWeapon()) {
               return false;
            }
         }

         if (this._items.isEmpty()) {
            for (EnchantScroll scroll : EnchantItemData.getInstance().getScrolls()) {
               if (scroll.getId() != this.getId()) {
                  Collection<Integer> scrollItems = scroll.getItems();
                  if (!scrollItems.isEmpty() && scrollItems.contains(itemToEnchant.getId())) {
                     return false;
                  }
               }
            }
         }

         return super.isValid(itemToEnchant, supportItem);
      }
   }

   public double getChance(Player player, Item enchantItem) {
      if (enchantItem == null) {
         return -1.0;
      } else {
         int scrollGroupId = this._items.getOrDefault(enchantItem.getId(), this._scrollGroupId);
         if (EnchantItemGroupsData.getInstance().getScrollGroup(scrollGroupId) == null) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Unexistent enchant scroll group specified for enchant scroll: " + this.getId());
            return -1.0;
         } else {
            EnchantItemGroup group = EnchantItemGroupsData.getInstance().getItemGroup(enchantItem.getTemplate(), scrollGroupId);
            if (group == null) {
               LOGGER.warning(this.getClass().getSimpleName() + ": Couldn't find enchant item group for scroll: " + this.getId() + " requested by: " + player);
               return -1.0;
            } else {
               return group.getChance(enchantItem.getEnchantLevel());
            }
         }
      }
   }

   public EnchantResultType calculateSuccess(Player player, Item enchantItem, EnchantSupportItem supportItem) {
      if (!this.isValid(enchantItem, supportItem)) {
         return EnchantResultType.ERROR;
      } else {
         double chance = this.getChance(player, enchantItem);
         if (chance == -1.0) {
            return EnchantResultType.ERROR;
         } else {
            double bonusRate = this.getBonusRate();
            double supportBonusRate = supportItem != null ? supportItem.getBonusRate() : 0.0;
            double finalChance = Math.min(chance + bonusRate + supportBonusRate, 100.0);
            double random = 100.0 * Rnd.nextDouble();
            boolean success = random < finalChance;
            return success ? EnchantResultType.SUCCESS : EnchantResultType.FAILURE;
         }
      }
   }
}
