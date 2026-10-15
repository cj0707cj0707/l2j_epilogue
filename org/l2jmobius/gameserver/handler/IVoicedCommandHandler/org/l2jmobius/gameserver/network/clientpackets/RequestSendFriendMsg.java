package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.L2FriendSay;

public class RequestSendFriendMsg extends ClientPacket {
   private static Logger LOGGER_CHAT = Logger.getLogger("chat");
   private String _message;
   private String _receiver;

   @Override
   protected void readImpl() {
      this._message = this.readString();
      this._receiver = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._message != null && !this._message.isEmpty() && this._message.length() <= 300) {
            Player targetPlayer = World.getPlayer(this._receiver);
            if (targetPlayer != null && targetPlayer.getFriendList().contains(player.getObjectId())) {
               if (GeneralConfig.LOG_CHAT) {
                  StringBuilder sb = new StringBuilder();
                  sb.append("PRIV_MSG [");
                  sb.append(player);
                  sb.append(" to ");
                  sb.append(targetPlayer);
                  sb.append("] ");
                  sb.append(this._message);
                  LOGGER_CHAT.info(sb.toString());
               }

               targetPlayer.sendPacket(new L2FriendSay(player.getName(), this._receiver, this._message));
            } else {
               player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
            }
         }
      }
   }
}
