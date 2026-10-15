package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;

public class RequestWithdrawPartyRoom extends ClientPacket {
   private int _roomid;
   private int _unk1;

   @Override
   protected void readImpl() {
      this._roomid = this.readInt();
      this._unk1 = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(this._roomid);
         if (room != null) {
            if (player.isInParty() && room.getOwner().isInParty() && player.getParty().getLeaderObjectId() == room.getOwner().getParty().getLeaderObjectId()) {
               player.broadcastUserInfo();
            } else {
               room.deleteMember(player);
               player.setPartyRoom(0);
               player.sendPacket(new ExClosePartyRoom());
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXITED_FROM_THE_PARTY_ROOM);
            }
         }
      }
   }
}
