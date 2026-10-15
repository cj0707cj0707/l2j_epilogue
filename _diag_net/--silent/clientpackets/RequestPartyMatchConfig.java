package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ListPartyWating;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;

public class RequestPartyMatchConfig extends ClientPacket {
   private int _auto;
   private int _loc;
   private int _level;

   @Override
   protected void readImpl() {
      this._auto = this.readInt();
      this._loc = this.readInt();
      this._level = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.isInPartyMatchRoom() && player.getParty() != null && player.getParty().getLeader() != player) {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.THE_LIST_OF_PARTY_ROOMS_CAN_ONLY_BE_VIEWED_BY_A_PERSON_WHO_HAS_NOT_JOINED_A_PARTY_OR_WHO_IS_CURRENTLY_THE_LEADER_OF_A_PARTY
            );
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            if (player.isInPartyMatchRoom()) {
               PartyMatchRoomList list = PartyMatchRoomList.getInstance();
               if (list == null) {
                  return;
               }

               PartyMatchRoom room = list.getPlayerRoom(player);
               if (room == null) {
                  return;
               }

               player.sendPacket(new PartyMatchDetail(room));
               player.sendPacket(new ExPartyRoomMember(room, 2));
               player.setPartyRoom(room.getId());
               player.broadcastUserInfo();
            } else {
               PartyMatchWaitingList.getInstance().addPlayer(player);
               player.sendPacket(new ListPartyWating(player, this._auto, this._loc, this._level));
            }
         }
      }
   }
}
