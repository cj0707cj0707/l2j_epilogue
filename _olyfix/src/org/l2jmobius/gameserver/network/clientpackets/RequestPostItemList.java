package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExReplyPostItemList;

public class RequestPostItemList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      if (GeneralConfig.ALLOW_MAIL && GeneralConfig.ALLOW_ATTACHMENTS) {
         Player player = this.getPlayer();
         if (player != null) {
            if (!player.isInsideZone(ZoneId.PEACE)) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
            } else {
               player.sendPacket(new ExReplyPostItemList(player));
            }
         }
      }
   }
}
