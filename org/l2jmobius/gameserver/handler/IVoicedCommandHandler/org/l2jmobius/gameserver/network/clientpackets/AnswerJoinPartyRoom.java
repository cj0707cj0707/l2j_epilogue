package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class AnswerJoinPartyRoom extends ClientPacket {
   private int _answer;

   @Override
   protected void readImpl() {
      this._answer = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player partner = player.getActiveRequester();
         if (partner == null) {
            player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
            player.setActiveRequester(null);
         } else if (World.getPlayer(partner.getObjectId()) == null) {
            player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
            player.setActiveRequester(null);
         } else {
            if (this._answer == 1 && !partner.isRequestExpired()) {
               PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(partner.getPartyRoom());
               if (room == null) {
                  return;
               }

               if (player.getLevel() >= room.getMinLevel() && player.getLevel() <= room.getMaxLevel()) {
                  PartyMatchWaitingList.getInstance().removePlayer(player);
                  player.setPartyRoom(partner.getPartyRoom());
                  player.sendPacket(new PartyMatchDetail(room));
                  player.sendPacket(new ExPartyRoomMember(room, 0));

                  for (Player member : room.getPartyMembers()) {
                     if (member != null) {
                        member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
                        member.sendPacket(new SystemMessage(SystemMessageId.C1_HAS_ENTERED_THE_PARTY_ROOM).addPcName(player));
                     }
                  }

                  room.addMember(player);
                  player.broadcastUserInfo();
               } else {
                  player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_THAT_PARTY_ROOM);
               }
            } else {
               partner.sendPacket(SystemMessageId.THE_RECIPIENT_OF_YOUR_INVITATION_DID_NOT_ACCEPT_THE_PARTY_MATCHING_INVITATION);
            }

            player.setActiveRequester(null);
            partner.onTransactionResponse();
         }
      }
   }
}
