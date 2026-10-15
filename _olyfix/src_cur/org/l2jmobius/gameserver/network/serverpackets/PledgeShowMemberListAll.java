package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeShowMemberListAll extends ServerPacket {
   private final Clan _clan;
   private final Player _player;
   private final Collection<ClanMember> _members;
   private int _pledgeType;

   public PledgeShowMemberListAll(Clan clan, Player player) {
      this._clan = clan;
      this._player = player;
      this._members = this._clan.getMembers();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      this._pledgeType = 0;
      this.writePledge(0, buffer);

      for (Clan.SubPledge subPledge : this._clan.getAllSubPledges()) {
         this._player.sendPacket(new PledgeReceiveSubPledgeCreated(subPledge, this._clan));
      }

      for (ClanMember m : this._members) {
         if (m.getPledgeType() != 0) {
            this._player.sendPacket(new PledgeShowMemberListAdd(m));
         }
      }

      this._player.updateUserInfo();
   }

   private void writePledge(int mainOrSubpledge, WriteBuffer buffer) {
      ServerPackets.PLEDGE_SHOW_MEMBER_LIST_ALL.writeId(this, buffer);
      buffer.writeInt(mainOrSubpledge);
      buffer.writeInt(this._clan.getId());
      buffer.writeInt(this._pledgeType);
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
      buffer.writeInt(this._clan.getSubPledgeMembersCount(this._pledgeType));

      for (ClanMember m : this._members) {
         if (m.getPledgeType() == this._pledgeType) {
            buffer.writeString(m.getName());
            buffer.writeInt(m.getLevel());
            buffer.writeInt(m.getClassId());
            Player player = m.getPlayer();
            if (player != null) {
               buffer.writeInt(player.getAppearance().isFemale());
               buffer.writeInt(player.getRace().ordinal());
            } else {
               buffer.writeInt(1);
               buffer.writeInt(1);
            }

            buffer.writeInt(m.isOnline() ? m.getObjectId() : 0);
            buffer.writeInt(m.getSponsor() != 0);
         }
      }
   }
}
