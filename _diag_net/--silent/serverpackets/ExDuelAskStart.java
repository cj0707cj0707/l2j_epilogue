package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExDuelAskStart extends ServerPacket {
   private final String _requestorName;
   private final int _partyDuel;

   public ExDuelAskStart(String requestor, int partyDuel) {
      this._requestorName = requestor;
      this._partyDuel = partyDuel;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_DUEL_ASK_START.writeId(this, buffer);
      buffer.writeString(this._requestorName);
      buffer.writeInt(this._partyDuel);
   }
}
