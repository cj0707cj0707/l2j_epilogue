package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExAskJoinPartyRoom extends ServerPacket {
   private final String _charName;

   public ExAskJoinPartyRoom(String charName) {
      this._charName = charName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_ASK_JOIN_PARTY_ROOM.writeId(this, buffer);
      buffer.writeString(this._charName);
   }
}
