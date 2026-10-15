package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;

public class SetPrivateStoreWholeMsg extends ClientPacket {
   private static final int MAX_MSG_LENGTH = 29;
   private String _msg;

   @Override
   protected void readImpl() {
      this._msg = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && player.getSellList() != null) {
         if (this._msg != null && this._msg.length() > 29) {
            PunishmentManager.handleIllegalPlayerAction(player, player + " tried to overflow private store whole message", GeneralConfig.DEFAULT_PUNISH);
         } else {
            player.getSellList().setTitle(this._msg);
            player.sendPacket(new ExPrivateStoreSetWholeMsg(player));
         }
      }
   }
}
