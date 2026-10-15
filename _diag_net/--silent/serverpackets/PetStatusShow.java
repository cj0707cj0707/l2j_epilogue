package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;

public class PetStatusShow extends ServerPacket {
   private final int _summonType;

   public PetStatusShow(Summon summon) {
      this._summonType = summon.getSummonType();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PET_STATUS_SHOW.writeId(this, buffer);
      buffer.writeInt(this._summonType);
   }
}
