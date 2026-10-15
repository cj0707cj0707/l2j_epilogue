package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPetitionCancel extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (PetitionManager.getInstance().isPlayerInConsultation(player)) {
            if (player.isGM()) {
               PetitionManager.getInstance().endActivePetition(player);
            } else {
               player.sendPacket(SystemMessageId.PETITION_UNDER_PROCESS);
            }
         } else if (PetitionManager.getInstance().isPlayerPetitionPending(player)) {
            if (PetitionManager.getInstance().cancelActivePetition(player)) {
               int numRemaining = PlayerConfig.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(player);
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_PETITION_WAS_CANCELED_YOU_MAY_SUBMIT_S1_MORE_PETITION_S_TODAY);
               sm.addString(String.valueOf(numRemaining));
               player.sendPacket(sm);
               String msgContent = player.getName() + " has canceled a pending petition.";
               AdminData.getInstance().broadcastToGMs(new CreatureSay(player, ChatType.HERO_VOICE, "Petition System", msgContent));
            } else {
               player.sendPacket(SystemMessageId.FAILED_TO_CANCEL_PETITION_PLEASE_TRY_AGAIN_LATER);
            }
         } else {
            player.sendPacket(SystemMessageId.YOU_HAVE_NOT_SUBMITTED_A_PETITION);
         }
      }
   }
}
