package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.RecipeManager;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class RequestRecipeBookOpen extends ClientPacket {
   private boolean _isDwarvenCraft;

   @Override
   protected void readImpl() {
      this._isDwarvenCraft = this.readInt() == 0;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isCastingNow() || player.isCastingSimultaneouslyNow()) {
            player.sendPacket(SystemMessageId.A_RECIPE_BOOK_MAY_NOT_BE_USED_WHILE_USING_A_SKILL);
         } else if (player.getActiveRequester() != null) {
            player.sendMessage("You may not alter your recipe book while trading.");
         } else {
            RecipeManager.getInstance().requestBookOpen(player, this._isDwarvenCraft);
         }
      }
   }
}
