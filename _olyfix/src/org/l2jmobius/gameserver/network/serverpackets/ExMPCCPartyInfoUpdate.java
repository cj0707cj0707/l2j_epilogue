package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExMPCCPartyInfoUpdate extends ServerPacket {
   private final int _mode;
   private final int _LeaderOID;
   private final int _memberCount;
   private final String _name;

   public ExMPCCPartyInfoUpdate(Party party, int mode) {
      this._name = party.getLeader().getName();
      this._LeaderOID = party.getLeaderObjectId();
      this._memberCount = party.getMemberCount();
      this._mode = mode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_MPCC_PARTY_INFO_UPDATE.writeId(this, buffer);
      buffer.writeString(this._name);
      buffer.writeInt(this._LeaderOID);
      buffer.writeInt(this._memberCount);
      buffer.writeInt(this._mode);
   }
}
