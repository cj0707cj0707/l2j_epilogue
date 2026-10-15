package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;

public class SetPrivateStoreMsgBuy extends ClientPacket {
   private static final int MAX_MSG_LENGTH = 29;
   private String _storeMsg;

   @Override
   protected void readImpl() {
      this._storeMsg = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && player.getBuyList() != null) {
         if (this._storeMsg != null && this._storeMsg.length() > 29) {
            PunishmentManager.handleIllegalPlayerAction(player, player + " tried to overflow private store buy message", GeneralConfig.DEFAULT_PUNISH);
         } else {
            player.getBuyList().setTitle(this._storeMsg);
            player.sendPacket(new PrivateStoreMsgBuy(player));
         }
      }
   }
}
