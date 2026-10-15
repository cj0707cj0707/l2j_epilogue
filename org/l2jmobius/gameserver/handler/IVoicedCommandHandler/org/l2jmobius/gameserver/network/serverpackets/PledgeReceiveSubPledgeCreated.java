package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeReceiveSubPledgeCreated extends ServerPacket {
   private final Clan.SubPledge _subPledge;
   private final Clan _clan;

   public PledgeReceiveSubPledgeCreated(Clan.SubPledge subPledge, Clan clan) {
      this._subPledge = subPledge;
      this._clan = clan;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_RECEIVE_SUB_PLEDGE_CREATED.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._subPledge.getId());
      buffer.writeString(this._subPledge.getName());
      buffer.writeString(this.getLeaderName());
   }

   private String getLeaderName() {
      int LeaderId = this._subPledge.getLeaderId();
      if (this._subPledge.getId() == -1 || LeaderId == 0) {
         return "";
      } else if (this._clan.getClanMember(LeaderId) == null) {
         PacketLogger.warning("SubPledgeLeader: " + LeaderId + " is missing from clan: " + this._clan.getName() + "[" + this._clan.getId() + "]");
         return "";
      } else {
         return this._clan.getClanMember(LeaderId).getName();
      }
   }
}
