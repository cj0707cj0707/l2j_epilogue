package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;

public class ExPartyPetWindowDelete extends ServerPacket {
   private final Summon _summon;

   public ExPartyPetWindowDelete(Summon summon) {
      this._summon = summon;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PARTY_PET_WINDOW_DELETE.writeId(this, buffer);
      buffer.writeInt(this._summon.getObjectId());
      buffer.writeInt(this._summon.getOwner().getObjectId());
      buffer.writeString(this._summon.getName());
   }
}
