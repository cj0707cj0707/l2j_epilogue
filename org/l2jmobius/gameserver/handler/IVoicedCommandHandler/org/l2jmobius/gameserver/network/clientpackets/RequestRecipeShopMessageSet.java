package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PunishmentManager;

public class RequestRecipeShopMessageSet extends ClientPacket {
   private static final int MAX_MSG_LENGTH = 29;
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._name != null && this._name.length() > 29) {
            PunishmentManager.handleIllegalPlayerAction(player, player + " tried to overflow recipe shop message", GeneralConfig.DEFAULT_PUNISH);
         } else {
            player.setStoreName(this._name);
         }
      }
   }
}
