package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExManagePartyRoomMember extends ServerPacket {
   private final Player _player;
   private final PartyMatchRoom _room;
   private final int _mode;

   public ExManagePartyRoomMember(Player player, PartyMatchRoom room, int mode) {
      this._player = player;
      this._room = room;
      this._mode = mode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_MANAGE_PARTY_ROOM_MEMBER.writeId(this, buffer);
      buffer.writeInt(this._mode);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._player.getActiveClass());
      buffer.writeInt(this._player.getLevel());
      buffer.writeInt(this._room.getLocation());
      if (this._room.getOwner().equals(this._player)) {
         buffer.writeInt(1);
      } else if (this._room.getOwner().isInParty()
         && this._player.isInParty()
         && this._room.getOwner().getParty().getLeaderObjectId() == this._player.getParty().getLeaderObjectId()) {
         buffer.writeInt(2);
      } else {
         buffer.writeInt(0);
      }
   }
}
