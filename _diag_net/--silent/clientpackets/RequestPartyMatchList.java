package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;

public class RequestPartyMatchList extends ClientPacket {
   private int _roomid;
   private int _membersmax;
   private int _minLevel;
   private int _maxLevel;
   private int _loot;
   private String _roomtitle;

   @Override
   protected void readImpl() {
      this._roomid = this.readInt();
      this._membersmax = this.readInt();
      this._minLevel = this.readInt();
      this._maxLevel = this.readInt();
      this._loot = this.readInt();
      this._roomtitle = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._roomid > 0) {
            PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(this._roomid);
            if (room != null) {
               room.setMaxMembers(this._membersmax);
               room.setMinLevel(this._minLevel);
               room.setMaxLevel(this._maxLevel);
               room.setLootType(this._loot);
               room.setTitle(this._roomtitle);

               for (Player member : room.getPartyMembers()) {
                  if (member != null) {
                     member.sendPacket(new PartyMatchDetail(room));
                     member.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_PARTY_ROOM_S_INFORMATION_HAS_BEEN_REVISED);
                  }
               }
            }
         } else {
            int maxid = PartyMatchRoomList.getInstance().getMaxId();
            PartyMatchRoom room = new PartyMatchRoom(maxid, this._roomtitle, this._loot, this._minLevel, this._maxLevel, this._membersmax, player);
            PartyMatchWaitingList.getInstance().removePlayer(player);
            PartyMatchRoomList.getInstance().addPartyMatchRoom(maxid, room);
            if (player.isInParty()) {
               for (Player ptmember : player.getParty().getMembers()) {
                  if (ptmember != null && ptmember != player) {
                     ptmember.setPartyRoom(maxid);
                     room.addMember(ptmember);
                  }
               }
            }

            player.sendPacket(new PartyMatchDetail(room));
            player.sendPacket(new ExPartyRoomMember(room, 1));
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_PARTY_ROOM_HAS_BEEN_CREATED);
            player.setPartyRoom(maxid);
            player.broadcastUserInfo();
         }
      }
   }
}
