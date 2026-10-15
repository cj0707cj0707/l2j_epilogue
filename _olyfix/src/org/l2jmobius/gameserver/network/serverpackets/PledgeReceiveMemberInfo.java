package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeReceiveMemberInfo extends ServerPacket {
   private final ClanMember _member;

   public PledgeReceiveMemberInfo(ClanMember member) {
      this._member = member;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_RECEIVE_MEMBER_INFO.writeId(this, buffer);
      buffer.writeInt(this._member.getPledgeType());
      buffer.writeString(this._member.getName());
      buffer.writeString(this._member.getTitle());
      buffer.writeInt(this._member.getPowerGrade());
      if (this._member.getPledgeType() != 0) {
         buffer.writeString(this._member.getClan().getSubPledge(this._member.getPledgeType()).getName());
      } else {
         buffer.writeString(this._member.getClan().getName());
      }

      buffer.writeString(this._member.getApprenticeOrSponsorName());
   }
}
