package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.ListPartyWating;

public class RequestOustFromPartyRoom extends ClientPacket {
   private int _charid;

   @Override
   protected void readImpl() {
      this._charid = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player member = World.getPlayer(this._charid);
         if (member != null) {
            PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(member);
            if (room != null && room.getOwner() == player) {
               if (player.isInParty() && member.isInParty() && player.getParty().getLeaderObjectId() == member.getParty().getLeaderObjectId()) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_DISMISS_A_PARTY_MEMBER_BY_FORCE);
               } else {
                  room.deleteMember(member);
                  member.setPartyRoom(0);
                  member.sendPacket(new ExClosePartyRoom());
                  PartyMatchWaitingList.getInstance().addPlayer(member);
                  int loc = 0;
                  member.sendPacket(new ListPartyWating(member, 0, 0, member.getLevel()));
                  member.broadcastUserInfo();
                  member.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_OUSTED_FROM_THE_PARTY_ROOM);
               }
            }
         }
      }
   }
}
