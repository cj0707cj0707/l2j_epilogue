package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExReplySentPost;

public class RequestSentPost extends ClientPacket {
   private int _msgId;

   @Override
   protected void readImpl() {
      this._msgId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && GeneralConfig.ALLOW_MAIL) {
         MailMessage msg = MailManager.getInstance().getMessage(this._msgId);
         if (msg != null) {
            if (!player.isInsideZone(ZoneId.PEACE) && msg.hasAttachments()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
            } else if (msg.getSenderId() != player.getObjectId()) {
               PunishmentManager.handleIllegalPlayerAction(player, player + " tried to read not own post!", GeneralConfig.DEFAULT_PUNISH);
            } else if (!msg.isDeletedBySender()) {
               player.sendPacket(new ExReplySentPost(msg));
            }
         }
      }
   }
}
