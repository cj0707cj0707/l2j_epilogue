package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.JoinParty;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAnswerJoinParty extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getActiveRequester();
         if (requestor != null) {
            requestor.sendPacket(new JoinParty(this._response));
            switch (this._response) {
               case -1:
                  requestor.sendMessage(player.getName() + " is set to refuse party requests and cannot receive a party request.");
               case 0:
               default:
                  break;
               case 1:
                  if (requestor.isInParty()) {
                     if (requestor.getParty().getMemberCount() >= 9) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.THE_PARTY_IS_FULL);
                        player.sendPacket(sm);
                        requestor.sendPacket(sm);
                        return;
                     }

                     player.joinParty(requestor.getParty());
                  } else {
                     requestor.setParty(new Party(requestor, requestor.getPartyDistributionType()));
                     player.joinParty(requestor.getParty());
                  }

                  if (requestor.isInPartyMatchRoom() && player.isInPartyMatchRoom()) {
                     PartyMatchRoomList list = PartyMatchRoomList.getInstance();
                     if (list != null && list.getPlayerRoomId(requestor) == list.getPlayerRoomId(player)) {
                        PartyMatchRoom room = list.getPlayerRoom(requestor);
                        if (room != null) {
                           ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);

                           for (Player member : room.getPartyMembers()) {
                              if (member != null) {
                                 member.sendPacket(packet);
                              }
                           }
                        }
                     }
                  } else if (requestor.isInPartyMatchRoom() && !player.isInPartyMatchRoom()) {
                     PartyMatchRoomList list = PartyMatchRoomList.getInstance();
                     if (list != null) {
                        PartyMatchRoom room = list.getPlayerRoom(requestor);
                        if (room != null) {
                           room.addMember(player);
                           ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);

                           for (Player memberx : room.getPartyMembers()) {
                              if (memberx != null) {
                                 memberx.sendPacket(packet);
                              }
                           }

                           player.setPartyRoom(room.getId());
                           player.broadcastUserInfo();
                        }
                     }
                  }
            }

            if (requestor.isInParty()) {
               requestor.getParty().setPendingInvitation(false);
            }

            player.setActiveRequester(null);
            requestor.onTransactionResponse();
         }
      }
   }
}
