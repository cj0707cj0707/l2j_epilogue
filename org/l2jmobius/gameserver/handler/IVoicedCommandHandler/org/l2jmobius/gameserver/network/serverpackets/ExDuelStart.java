package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExDuelStart extends ServerPacket {
   public static final ExDuelStart PLAYER_DUEL = new ExDuelStart(false);
   public static final ExDuelStart PARTY_DUEL = new ExDuelStart(true);
   private final boolean _partyDuel;

   public ExDuelStart(boolean isPartyDuel) {
      this._partyDuel = isPartyDuel;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_DUEL_START.writeId(this, buffer);
      buffer.writeInt(this._partyDuel);
   }
}
