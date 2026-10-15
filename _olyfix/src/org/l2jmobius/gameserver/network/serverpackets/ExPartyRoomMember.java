package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPartyRoomMember extends ServerPacket {
   private final PartyMatchRoom _room;
   private final int _mode;

   public ExPartyRoomMember(PartyMatchRoom room, int mode) {
      this._room = room;
      this._mode = mode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PARTY_ROOM_MEMBER.writeId(this, buffer);
      buffer.writeInt(this._mode);
      buffer.writeInt(this._room.getMembers());

      for (Player member : this._room.getPartyMembers()) {
         buffer.writeInt(member.getObjectId());
         buffer.writeString(member.getName());
         buffer.writeInt(member.getActiveClass());
         buffer.writeInt(member.getLevel());
         buffer.writeInt(this._room.getLocation());
         if (this._room.getOwner().equals(member)) {
            buffer.writeInt(1);
         } else if (this._room.getOwner().isInParty()
            && member.isInParty()
            && this._room.getOwner().getParty().getLeaderObjectId() == member.getParty().getLeaderObjectId()) {
            buffer.writeInt(2);
         } else {
            buffer.writeInt(0);
         }

         buffer.writeInt(0);
      }
   }
}
