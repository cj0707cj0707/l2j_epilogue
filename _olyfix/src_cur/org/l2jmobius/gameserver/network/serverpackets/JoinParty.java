package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class JoinParty extends ServerPacket {
   private final int _response;

   public JoinParty(int response) {
      this._response = response;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.JOIN_PARTY.writeId(this, buffer);
      buffer.writeInt(this._response);
   }
}
