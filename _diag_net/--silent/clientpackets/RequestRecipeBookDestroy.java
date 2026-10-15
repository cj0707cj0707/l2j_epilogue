package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;

public class RequestRecipeBookDestroy extends ClientPacket {
   private int _recipeID;

   @Override
   protected void readImpl() {
      this._recipeID = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            RecipeList rp = RecipeData.getInstance().getRecipeList(this._recipeID);
            if (rp != null) {
               player.unregisterRecipeList(this._recipeID);
               RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), player.getMaxMp());
               if (rp.isDwarvenRecipe()) {
                  response.addRecipes(player.getDwarvenRecipeBook());
               } else {
                  response.addRecipes(player.getCommonRecipeBook());
               }

               player.sendPacket(response);
            }
         }
      }
   }
}
