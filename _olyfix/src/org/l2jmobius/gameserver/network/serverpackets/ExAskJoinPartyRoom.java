package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExAskJoinPartyRoom extends ServerPacket {
   private final String _charName;

   public ExAskJoinPartyRoom(String charName) {
      this._charName = charName;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_ASK_JOIN_PARTY_ROOM.writeId(this, buffer);
      buffer.writeString(this._charName);
   }
}
