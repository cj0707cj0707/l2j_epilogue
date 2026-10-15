package org.l2jmobius.gameserver.mechanics.multisell;

import java.util.ArrayList;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class PreparedEntry extends Entry {
   private long _taxAmount = 0L;

   public PreparedEntry(Entry template, Item item, boolean applyTaxes, boolean maintainEnchantment, double taxRate) {
      this._entryId = template.getEntryId() * 100000;
      if (maintainEnchantment && item != null) {
         this._entryId = this._entryId + item.getEnchantLevel();
      }

      ItemInfo info = null;
      long adenaAmount = 0L;
      this._ingredients = new ArrayList<>(template.getIngredients().size());

      for (Ingredient ing : template.getIngredients()) {
         if (ing.getItemId() == 57) {
            if (ing.isTaxIngredient()) {
               if (applyTaxes) {
                  this._taxAmount = this._taxAmount + Math.round(ing.getItemCount() * taxRate);
               }
            } else {
               adenaAmount += ing.getItemCount();
            }
         } else if (maintainEnchantment && item != null && ing.isArmorOrWeapon()) {
            info = new ItemInfo(item);
            Ingredient newIngredient = ing.getCopy();
            newIngredient.setItemInfo(info);
            this._ingredients.add(newIngredient);
         } else {
            this._ingredients.add(ing.getCopy());
         }
      }

      adenaAmount += this._taxAmount;
      if (adenaAmount > 0L) {
         this._ingredients.add(new Ingredient(57, adenaAmount, 0, false, false));
      }

      this._products = new ArrayList<>(template.getProducts().size());

      for (Ingredient ingx : template.getProducts()) {
         if (!ingx.isStackable()) {
            this._stackable = false;
         }

         Ingredient newProduct = ingx.getCopy();
         if (maintainEnchantment && ingx.isArmorOrWeapon()) {
            newProduct.setItemInfo(info);
         } else if (ingx.isArmorOrWeapon() && ingx.getTemplate().getDefaultEnchantLevel() > 0) {
            info = new ItemInfo(ingx.getTemplate().getDefaultEnchantLevel());
            newProduct.setItemInfo(info);
         }

         this._products.add(newProduct);
      }
   }

   @Override
   public long getTaxAmount() {
      return this._taxAmount;
   }
}
