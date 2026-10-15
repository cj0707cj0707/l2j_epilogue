package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PartySmallWindowUpdate extends ServerPacket {
   private final Player _member;

   public PartySmallWindowUpdate(Player member) {
      this._member = member;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PARTY_SMALL_WINDOW_UPDATE.writeId(this, buffer);
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
   }
}
