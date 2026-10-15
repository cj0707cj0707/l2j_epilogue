package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class JoinParty extends ServerPacket {
   private final int _response;

   public JoinParty(int response) {
      this._response = response;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.JOIN_PARTY.writeId(this, buffer);
      buffer.writeInt(this._response);
   }
}
