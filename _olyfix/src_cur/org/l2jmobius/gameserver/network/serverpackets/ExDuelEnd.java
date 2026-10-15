package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExDuelEnd extends ServerPacket {
   public static final ExDuelEnd PLAYER_DUEL = new ExDuelEnd(false);
   public static final ExDuelEnd PARTY_DUEL = new ExDuelEnd(true);
   private final boolean _partyDuel;

   public ExDuelEnd(boolean isPartyDuel) {
      this._partyDuel = isPartyDuel;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_DUEL_END.writeId(this, buffer);
      buffer.writeInt(this._partyDuel);
   }
}
