package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;

public class ExPartyPetWindowAdd extends ServerPacket {
   private final Summon _summon;

   public ExPartyPetWindowAdd(Summon summon) {
      this._summon = summon;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PARTY_PET_WINDOW_ADD.writeId(this, buffer);
      buffer.writeInt(this._summon.getObjectId());
      buffer.writeInt(this._summon.getTemplate().getDisplayId() + 1000000);
      buffer.writeInt(this._summon.getSummonType());
      buffer.writeInt(this._summon.getOwner().getObjectId());
      buffer.writeString(this._summon.getName());
      buffer.writeInt((int)this._summon.getCurrentHp());
      buffer.writeInt(this._summon.getMaxHp());
      buffer.writeInt((int)this._summon.getCurrentMp());
      buffer.writeInt(this._summon.getMaxMp());
      buffer.writeInt(this._summon.getLevel());
   }
}
