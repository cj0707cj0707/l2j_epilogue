package org.l2jmobius.gameserver.entity.item.recipe;

import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.holders.RecipeStatHolder;
import org.l2jmobius.gameserver.util.StatSet;

public class RecipeList {
   private RecipeHolder[] _recipes = new RecipeHolder[0];
   private RecipeStatHolder[] _statUse = new RecipeStatHolder[0];
   private RecipeStatHolder[] _altStatChange = new RecipeStatHolder[0];
   private final int _id;
   private final int _level;
   private final int _recipeId;
   private final String _recipeName;
   private final int _successRate;
   private final int _itemId;
   private final int _count;
   private int _rareItemId;
   private int _rareCount;
   private int _rarity;
   private final boolean _isDwarvenRecipe;

   public RecipeList(StatSet set, boolean haveRare) {
      this._id = set.getInt("id");
      this._level = set.getInt("craftLevel");
      this._recipeId = set.getInt("recipeId");
      this._recipeName = set.getString("recipeName");
      this._successRate = set.getInt("successRate");
      this._itemId = set.getInt("itemId");
      this._count = set.getInt("count");
      if (haveRare) {
         this._rareItemId = set.getInt("rareItemId");
         this._rareCount = set.getInt("rareCount");
         this._rarity = set.getInt("rarity");
      }

      this._isDwarvenRecipe = set.getBoolean("isDwarvenRecipe");
   }

   public void addRecipe(RecipeHolder recipe) {
      int len = this._recipes.length;
      RecipeHolder[] tmp = new RecipeHolder[len + 1];
      System.arraycopy(this._recipes, 0, tmp, 0, len);
      tmp[len] = recipe;
      this._recipes = tmp;
   }

   public void addStatUse(RecipeStatHolder statUse) {
      int len = this._statUse.length;
      RecipeStatHolder[] tmp = new RecipeStatHolder[len + 1];
      System.arraycopy(this._statUse, 0, tmp, 0, len);
      tmp[len] = statUse;
      this._statUse = tmp;
   }

   public void addAltStatChange(RecipeStatHolder statChange) {
      int len = this._altStatChange.length;
      RecipeStatHolder[] tmp = new RecipeStatHolder[len + 1];
      System.arraycopy(this._altStatChange, 0, tmp, 0, len);
      tmp[len] = statChange;
      this._altStatChange = tmp;
   }

   public int getId() {
      return this._id;
   }

   public int getLevel() {
      return this._level;
   }

   public int getRecipeId() {
      return this._recipeId;
   }

   public String getRecipeName() {
      return this._recipeName;
   }

   public int getSuccessRate() {
      return this._successRate;
   }

   public int getItemId() {
      return this._itemId;
   }

   public int getCount() {
      return this._count;
   }

   public int getRareItemId() {
      return this._rareItemId;
   }

   public int getRareCount() {
      return this._rareCount;
   }

   public int getRarity() {
      return this._rarity;
   }

   public boolean isDwarvenRecipe() {
      return this._isDwarvenRecipe;
   }

   public RecipeHolder[] getRecipes() {
      return this._recipes;
   }

   public RecipeStatHolder[] getStatUse() {
      return this._statUse;
   }

   public RecipeStatHolder[] getAltStatChange() {
      return this._altStatChange;
   }
}
