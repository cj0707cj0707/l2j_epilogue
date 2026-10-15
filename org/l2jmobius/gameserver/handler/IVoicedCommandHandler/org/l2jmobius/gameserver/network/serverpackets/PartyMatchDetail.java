package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartyMatchDetail extends ServerPacket {
   private final PartyMatchRoom _room;

   public PartyMatchDetail(PartyMatchRoom room) {
      this._room = room;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PARTY_ROOM_INFO.writeId(this, buffer);
      buffer.writeInt(this._room.getId());
      buffer.writeInt(this._room.getMaxMembers());
      buffer.writeInt(this._room.getMinLevel());
      buffer.writeInt(this._room.getMaxLevel());
      buffer.writeInt(this._room.getLootType());
      buffer.writeInt(this._room.getLocation());
      buffer.writeString(this._room.getTitle());
   }
}
