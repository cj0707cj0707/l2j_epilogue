package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartySmallWindowDelete extends ServerPacket {
   private final Player _member;

   public PartySmallWindowDelete(Player member) {
      this._member = member;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PARTY_SMALL_WINDOW_DELETE.writeId(this, buffer);
      buffer.writeInt(this._member.getObjectId());
      buffer.writeString(this._member.getName());
   }
}
