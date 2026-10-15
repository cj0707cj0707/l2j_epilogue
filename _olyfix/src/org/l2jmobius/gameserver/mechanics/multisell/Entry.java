package org.l2jmobius.gameserver.mechanics.multisell;

import java.util.ArrayList;
import java.util.List;

public class Entry {
   protected int _entryId;
   protected boolean _stackable = true;
   protected List<Ingredient> _products;
   protected List<Ingredient> _ingredients;

   public Entry(int entryId) {
      this._entryId = entryId;
      this._products = new ArrayList<>();
      this._ingredients = new ArrayList<>();
   }

   protected Entry() {
   }

   public void setEntryId(int id) {
      this._entryId = id;
   }

   public int getEntryId() {
      return this._entryId;
   }

   public void addProduct(Ingredient product) {
      this._products.add(product);
      if (!product.isStackable()) {
         this._stackable = false;
      }
   }

   public List<Ingredient> getProducts() {
      return this._products;
   }

   public void addIngredient(Ingredient ingredient) {
      this._ingredients.add(ingredient);
   }

   public List<Ingredient> getIngredients() {
      return this._ingredients;
   }

   public boolean isStackable() {
      return this._stackable;
   }

   public long getTaxAmount() {
      return 0L;
   }
}
