package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;

public class RequestWithDrawalParty extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Party party = player.getParty();
         if (party != null) {
            if (player.getUCState() != 0) {
               player.sendMessage("You can't dismiss party member when you are in Underground Coliseum.");
            } else if (party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(player)) {
               player.sendMessage("You can't exit party when you are in Dimensional Rift.");
            } else {
               party.removePartyMember(player, PartyMessageType.LEFT);
               if (player.isInPartyMatchRoom()) {
                  PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
                  if (room != null) {
                     player.sendPacket(new PartyMatchDetail(room));
                     player.sendPacket(new ExPartyRoomMember(room, 0));
                     player.sendPacket(new ExClosePartyRoom());
                     room.deleteMember(player);
                  }

                  player.setPartyRoom(0);
                  player.broadcastUserInfo();
               }
            }
         }
      }
   }
}
