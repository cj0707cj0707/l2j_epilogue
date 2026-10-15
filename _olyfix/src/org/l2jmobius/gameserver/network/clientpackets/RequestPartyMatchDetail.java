package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPartyMatchDetail extends ClientPacket {
   private int _roomid;
   private int _unk1;
   private int _unk2;
   private int _unk3;

   @Override
   protected void readImpl() {
      this._roomid = this.readInt();
      this._unk1 = this.readInt();
      this._unk2 = this.readInt();
      this._unk3 = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(this._roomid);
         if (room != null) {
            if (player.getLevel() >= room.getMinLevel() && player.getLevel() <= room.getMaxLevel()) {
               PartyMatchWaitingList.getInstance().removePlayer(player);
               player.setPartyRoom(this._roomid);
               player.sendPacket(new PartyMatchDetail(room));
               player.sendPacket(new ExPartyRoomMember(room, 0));

               for (Player member : room.getPartyMembers()) {
                  if (member != null) {
                     member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
                     SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_ENTERED_THE_PARTY_ROOM);
                     sm.addString(player.getName());
                     member.sendPacket(sm);
                  }
               }

               room.addMember(player);
               player.broadcastUserInfo();
            } else {
               player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_THAT_PARTY_ROOM);
            }
         }
      }
   }
}
