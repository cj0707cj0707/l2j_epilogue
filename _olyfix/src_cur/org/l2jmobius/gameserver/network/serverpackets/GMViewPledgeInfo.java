package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GMViewPledgeInfo extends ServerPacket {
   private final Clan _clan;
   private final Player _player;

   public GMViewPledgeInfo(Clan clan, Player player) {
      this._clan = clan;
      this._player = player;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.GM_VIEW_PLEDGE_INFO.writeId(this, buffer);
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._clan.getId());
      buffer.writeInt(0);
      buffer.writeString(this._clan.getName());
      buffer.writeString(this._clan.getLeaderName());
      buffer.writeInt(this._clan.getCrestId());
      buffer.writeInt(this._clan.getLevel());
      buffer.writeInt(this._clan.getCastleId());
      buffer.writeInt(this._clan.getHideoutId());
      buffer.writeInt(this._clan.getFortId());
      buffer.writeInt(this._clan.getRank());
      buffer.writeInt(this._clan.getReputationScore());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(this._clan.getAllyId());
      buffer.writeString(this._clan.getAllyName());
      buffer.writeInt(this._clan.getAllyCrestId());
      buffer.writeInt(this._clan.isAtWar());
      buffer.writeInt(0);
      Collection<ClanMember> members = this._clan.getMembers();
      buffer.writeInt(members.size());

      for (ClanMember member : members) {
         if (member != null) {
            buffer.writeString(member.getName());
            buffer.writeInt(member.getLevel());
            buffer.writeInt(member.getClassId());
            buffer.writeInt(member.getSex());
            buffer.writeInt(member.getRaceOrdinal());
            buffer.writeInt(member.isOnline() ? member.getObjectId() : 0);
            buffer.writeInt(member.getSponsor() != 0);
         }
      }
   }
}
