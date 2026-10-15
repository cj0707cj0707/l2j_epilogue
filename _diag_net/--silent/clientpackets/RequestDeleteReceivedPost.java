package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;

public class RequestDeleteReceivedPost extends ClientPacket {
   private static final int BATCH_LENGTH = 4;
   int[] _msgIds = null;

   @Override
   protected void readImpl() {
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 4 == this.remaining()) {
         this._msgIds = new int[count];

         for (int i = 0; i < count; i++) {
            this._msgIds[i] = this.readInt();
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && this._msgIds != null && GeneralConfig.ALLOW_MAIL) {
         if (!player.isInsideZone(ZoneId.PEACE)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_USE_THE_MAIL_FUNCTION_OUTSIDE_THE_PEACE_ZONE);
         } else {
            for (int msgId : this._msgIds) {
               MailMessage msg = MailManager.getInstance().getMessage(msgId);
               if (msg != null) {
                  if (msg.getReceiverId() != player.getObjectId()) {
                     PunishmentManager.handleIllegalPlayerAction(player, player + " tried to delete not own post!", GeneralConfig.DEFAULT_PUNISH);
                     return;
                  }

                  if (msg.hasAttachments() || msg.isDeletedByReceiver()) {
                     return;
                  }

                  msg.setDeletedByReceiver();
               }
            }

            player.sendPacket(new ExChangePostState(true, this._msgIds, 0));
         }
      }
   }
}
