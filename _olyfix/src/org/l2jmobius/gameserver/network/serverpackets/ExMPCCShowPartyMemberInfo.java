package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExMPCCShowPartyMemberInfo extends ServerPacket {
   private final Party _party;

   public ExMPCCShowPartyMemberInfo(Party party) {
      this._party = party;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_MPCC_SHOW_PARTY_MEMBER_INFO.writeId(this, buffer);
      buffer.writeInt(this._party.getMemberCount());

      for (Player pc : this._party.getMembers()) {
         buffer.writeString(pc.getName());
         buffer.writeInt(pc.getObjectId());
         buffer.writeInt(pc.getPlayerClass().getId());
      }
   }
}
