package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExDuelReady extends ServerPacket {
   public static final ExDuelReady PLAYER_DUEL = new ExDuelReady(false);
   public static final ExDuelReady PARTY_DUEL = new ExDuelReady(true);
   private final boolean _partyDuel;

   public ExDuelReady(boolean isPartyDuel) {
      this._partyDuel = isPartyDuel;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_DUEL_READY.writeId(this, buffer);
      buffer.writeInt(this._partyDuel);
   }
}
