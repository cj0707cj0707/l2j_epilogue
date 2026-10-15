package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartySmallWindowAdd extends ServerPacket {
   private final Player _member;
   private final Party _party;

   public PartySmallWindowAdd(Player member, Party party) {
      this._member = member;
      this._party = party;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PARTY_SMALL_WINDOW_ADD.writeId(this, buffer);
      buffer.writeInt(this._party.getLeaderObjectId());
      buffer.writeInt(this._party.getDistributionType().getId());
      buffer.writeInt(this._member.getObjectId());
      buffer.writeString(this._member.getName());
      buffer.writeInt((int)this._member.getCurrentCp());
      buffer.writeInt(this._member.getMaxCp());
      buffer.writeInt((int)this._member.getCurrentHp());
      buffer.writeInt(this._member.getMaxHp());
      buffer.writeInt((int)this._member.getCurrentMp());
      buffer.writeInt(this._member.getMaxMp());
      buffer.writeInt(this._member.getLevel());
      buffer.writeInt(this._member.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeInt(0);
   }
}
