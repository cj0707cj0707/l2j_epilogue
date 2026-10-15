package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestRejectPostAttachment extends ClientPacket {
   private int _msgId;

   @Override
   protected void readImpl() {
      this._msgId = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (GeneralConfig.ALLOW_MAIL && GeneralConfig.ALLOW_ATTACHMENTS) {
         Player player = this.getPlayer();
         if (player != null) {
            if (this.getClient().getFloodProtectors().canPerformTransaction()) {
               if (!player.isInsideZone(ZoneId.PEACE)) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
               } else {
                  MailMessage msg = MailManager.getInstance().getMessage(this._msgId);
                  if (msg != null) {
                     if (msg.getReceiverId() != player.getObjectId()) {
                        PunishmentManager.handleIllegalPlayerAction(player, player + " tried to reject not own attachment!", GeneralConfig.DEFAULT_PUNISH);
                     } else if (msg.hasAttachments() && msg.getSendBySystem() == 0) {
                        MailManager.getInstance().sendMessage(new MailMessage(msg));
                        player.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_RETURNED);
                        player.sendPacket(new ExChangePostState(true, this._msgId, 2));
                        Player sender = World.getPlayer(msg.getSenderId());
                        if (sender != null) {
                           SystemMessage sm = new SystemMessage(SystemMessageId.S1_RETURNED_THE_MAIL);
                           sm.addString(player.getName());
                           sender.sendPacket(sm);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
