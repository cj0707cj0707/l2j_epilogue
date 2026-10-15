package org.l2jmobius.gameserver.entity.item.recipe;

import org.l2jmobius.gameserver.data.xml.RecipeData;

public class ManufactureItem {
   private final int _recipeId;
   private final long _cost;
   private final boolean _isDwarven;

   public ManufactureItem(int recipeId, long cost) {
      this._recipeId = recipeId;
      this._cost = cost;
      this._isDwarven = RecipeData.getInstance().getRecipeList(this._recipeId).isDwarvenRecipe();
   }

   public int getRecipeId() {
      return this._recipeId;
   }

   public long getCost() {
      return this._cost;
   }

   public boolean isDwarven() {
      return this._isDwarven;
   }
}
