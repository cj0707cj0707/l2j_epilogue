package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExDuelReady extends ServerPacket {
   public static final ExDuelReady PLAYER_DUEL = new ExDuelReady(false);
   public static final ExDuelReady PARTY_DUEL = new ExDuelReady(true);
   private final boolean _partyDuel;

   public ExDuelReady(boolean isPartyDuel) {
      this._partyDuel = isPartyDuel;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_DUEL_READY.writeId(this, buffer);
      buffer.writeInt(this._partyDuel);
   }
}
