package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;

public class RequestDismissPartyRoom extends ClientPacket {
   private int _roomid;
   private int _data2;

   @Override
   protected void readImpl() {
      this._roomid = this.readInt();
      this._data2 = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(this._roomid);
         if (room != null) {
            PartyMatchRoomList.getInstance().deleteRoom(this._roomid);
         }
      }
   }
}
