package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class AutoAttackStop extends ServerPacket {
   private final int _targetObjId;

   public AutoAttackStop(int targetObjId) {
      this._targetObjId = targetObjId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.AUTO_ATTACK_STOP.writeId(this, buffer);
      buffer.writeInt(this._targetObjId);
   }

   public boolean canBeDropped(org.l2jmobius.gameserver.network.GameClient client) {
      return true;
   }
}
