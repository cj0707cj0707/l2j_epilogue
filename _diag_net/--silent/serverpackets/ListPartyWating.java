package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;

public class ListPartyWating extends ServerPacket {
   private final Player _player;
   private final int _loc;
   private final int _lim;
   private final List<PartyMatchRoom> _rooms;

   public ListPartyWating(Player player, int auto, int location, int limit) {
      this._player = player;
      this._loc = location;
      this._lim = limit;
      this._rooms = new ArrayList<>();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      for (PartyMatchRoom room : PartyMatchRoomList.getInstance().getRooms()) {
         if (room.getMembers() < 1 || room.getOwner() == null || !room.getOwner().isOnline() || room.getOwner().getPartyRoom() != room.getId()) {
            PartyMatchRoomList.getInstance().deleteRoom(room.getId());
         } else if ((this._loc <= 0 || this._loc == room.getLocation())
            && (this._lim != 0 || this._player.getLevel() >= room.getMinLevel() && this._player.getLevel() <= room.getMaxLevel())) {
            this._rooms.add(room);
         }
      }

      int size = this._rooms.size();
      org.l2jmobius.gameserver.network.ServerPackets.LIST_PARTY_WAITING.writeId(this, buffer);
      if (size > 0) {
         buffer.writeInt(1);
      } else {
         buffer.writeInt(0);
      }

      buffer.writeInt(this._rooms.size());

      for (PartyMatchRoom roomx : this._rooms) {
         buffer.writeInt(roomx.getId());
         buffer.writeString(roomx.getTitle());
         buffer.writeInt(roomx.getLocation());
         buffer.writeInt(roomx.getMinLevel());
         buffer.writeInt(roomx.getMaxLevel());
         buffer.writeInt(roomx.getMaxMembers());
         buffer.writeInt(roomx.getMembers());
         buffer.writeString(roomx.getOwner().getName());
      }
   }
}
