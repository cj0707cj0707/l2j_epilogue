package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class AutoAttackStop extends ServerPacket {
   private final int _targetObjId;

   public AutoAttackStop(int targetObjId) {
      this._targetObjId = targetObjId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.AUTO_ATTACK_STOP.writeId(this, buffer);
      buffer.writeInt(this._targetObjId);
   }

   public boolean canBeDropped(GameClient client) {
      return true;
   }
}
