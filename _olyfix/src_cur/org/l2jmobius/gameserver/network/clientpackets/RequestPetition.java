package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPetition extends ClientPacket {
   private String _content;
   private int _type;

   @Override
   protected void readImpl() {
      this._content = this.readString();
      this._type = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._type > 0 && this._type < 10) {
            if (!AdminData.getInstance().isGmOnline(false)) {
               player.sendPacket(SystemMessageId.THERE_ARE_NO_GMS_CURRENTLY_VISIBLE_IN_THE_PUBLIC_LIST_AS_THEY_MAY_BE_PERFORMING_OTHER_FUNCTIONS_AT_THE_MOMENT);
            } else if (!PetitionManager.getInstance().isPetitioningAllowed()) {
               player.sendPacket(SystemMessageId.THE_GAME_CLIENT_ENCOUNTERED_AN_ERROR_AND_WAS_UNABLE_TO_CONNECT_TO_THE_PETITION_SERVER);
            } else if (PetitionManager.getInstance().isPlayerPetitionPending(player)) {
               player.sendPacket(SystemMessageId.YOU_MAY_ONLY_SUBMIT_ONE_PETITION_ACTIVE_AT_A_TIME);
            } else if (PetitionManager.getInstance().getPendingPetitionCount() == PlayerConfig.MAX_PETITIONS_PENDING) {
               player.sendPacket(SystemMessageId.THE_PETITION_SYSTEM_IS_CURRENTLY_UNAVAILABLE_PLEASE_TRY_AGAIN_LATER);
            } else {
               int totalPetitions = PetitionManager.getInstance().getPlayerTotalPetitionCount(player) + 1;
               if (totalPetitions > PlayerConfig.MAX_PETITIONS_PER_PLAYER) {
                  SystemMessage sm = new SystemMessage(
                     SystemMessageId.WE_HAVE_RECEIVED_S1_PETITIONS_FROM_YOU_TODAY_AND_THAT_IS_THE_MAXIMUM_THAT_YOU_CAN_SUBMIT_IN_ONE_DAY_YOU_CANNOT_SUBMIT_ANY_MORE_PETITIONS
                  );
                  sm.addInt(totalPetitions);
                  player.sendPacket(sm);
               } else if (this._content.length() > 255) {
                  player.sendPacket(SystemMessageId.PETITIONS_CANNOT_EXCEED_255_CHARACTERS);
               } else {
                  int petitionId = PetitionManager.getInstance().submitPetition(player, this._content, this._type);
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PETITION_APPLICATION_HAS_BEEN_ACCEPTED_RECEIPT_NO_IS_S1);
                  sm.addInt(petitionId);
                  player.sendPacket(sm);
                  sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUBMITTED_S1_PETITION_S_YOU_MAY_SUBMIT_S2_MORE_PETITION_S_TODAY);
                  sm.addInt(totalPetitions);
                  sm.addInt(PlayerConfig.MAX_PETITIONS_PER_PLAYER - totalPetitions);
                  player.sendPacket(sm);
                  sm = new SystemMessage(SystemMessageId.THERE_ARE_S1_PETITIONS_CURRENTLY_ON_THE_WAITING_LIST);
                  sm.addInt(PetitionManager.getInstance().getPendingPetitionCount());
                  player.sendPacket(sm);
               }
            }
         }
      }
   }
}
