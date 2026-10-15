package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;

public class PartySmallWindowAll extends ServerPacket {
   private final Party _party;
   private final Player _exclude;

   public PartySmallWindowAll(Player exclude, Party party) {
      this._exclude = exclude;
      this._party = party;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PARTY_SMALL_WINDOW_ALL.writeId(this, buffer);
      buffer.writeInt(this._party.getLeaderObjectId());
      buffer.writeInt(this._party.getDistributionType().getId());
      buffer.writeInt(this._party.getMemberCount() - 1);

      for (Player member : this._party.getMembers()) {
         if (member != null && member != this._exclude) {
            buffer.writeInt(member.getObjectId());
            buffer.writeString(member.getName());
            buffer.writeInt((int)member.getCurrentCp());
            buffer.writeInt(member.getMaxCp());
            buffer.writeInt((int)member.getCurrentHp());
            buffer.writeInt(member.getMaxHp());
            buffer.writeInt((int)member.getCurrentMp());
            buffer.writeInt(member.getMaxMp());
            buffer.writeInt(member.getLevel());
            buffer.writeInt(member.getPlayerClass().getId());
            buffer.writeInt(0);
            buffer.writeInt(member.getRace().ordinal());
            buffer.writeInt(0);
            buffer.writeInt(0);
            if (member.hasSummon()) {
               buffer.writeInt(member.getSummon().getObjectId());
               buffer.writeInt(member.getSummon().getId() + 1000000);
               buffer.writeInt(member.getSummon().getSummonType());
               buffer.writeString(member.getSummon().getName());
               buffer.writeInt((int)member.getSummon().getCurrentHp());
               buffer.writeInt(member.getSummon().getMaxHp());
               buffer.writeInt((int)member.getSummon().getCurrentMp());
               buffer.writeInt(member.getSummon().getMaxMp());
               buffer.writeInt(member.getSummon().getLevel());
            } else {
               buffer.writeInt(0);
            }
         }
      }
   }
}
