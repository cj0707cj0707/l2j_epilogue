package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class AskJoinParty extends ServerPacket {
   private final String _requestorName;
   private final PartyDistributionType _partyDistributionType;

   public AskJoinParty(String requestorName, PartyDistributionType partyDistributionType) {
      this._requestorName = requestorName;
      this._partyDistributionType = partyDistributionType;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.ASK_JOIN_PARTY.writeId(this, buffer);
      buffer.writeString(this._requestorName);
      buffer.writeInt(this._partyDistributionType.getId());
   }
}
