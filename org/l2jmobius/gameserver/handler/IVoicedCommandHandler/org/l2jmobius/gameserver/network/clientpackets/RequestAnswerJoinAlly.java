package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class RequestAnswerJoinAlly extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getRequest().getPartner();
         if (requestor != null) {
            if (this._response == 0) {
               player.sendPacket(SystemMessageId.NO_RESPONSE_YOUR_ENTRANCE_TO_THE_ALLIANCE_HAS_BEEN_CANCELLED);
               requestor.sendPacket(SystemMessageId.NO_RESPONSE_INVITATION_TO_JOIN_AN_ALLIANCE_HAS_BEEN_CANCELLED);
            } else {
               if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinAlly)) {
                  return;
               }

               Clan requestorClan = requestor.getClan();
               if (requestorClan.checkAllyJoinCondition(requestor, player)) {
                  requestor.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_INVITING_A_FRIEND_TO_YOUR_FRIENDS_LIST);
                  player.sendPacket(SystemMessageId.YOU_HAVE_ACCEPTED_THE_ALLIANCE);
                  Clan clan = player.getClan();
                  clan.setAllyId(requestorClan.getAllyId());
                  clan.setAllyName(requestorClan.getAllyName());
                  clan.setAllyPenaltyExpiryTime(0L, 0);
                  clan.changeAllyCrest(requestorClan.getAllyCrestId(), true);
                  clan.updateClanInDB();
               }
            }

            player.getRequest().onRequestResponse();
         }
      }
   }
}
