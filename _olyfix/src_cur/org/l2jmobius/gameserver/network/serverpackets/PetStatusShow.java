package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PetStatusShow extends ServerPacket {
   private final int _summonType;

   public PetStatusShow(Summon summon) {
      this._summonType = summon.getSummonType();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PET_STATUS_SHOW.writeId(this, buffer);
      buffer.writeInt(this._summonType);
   }
}
