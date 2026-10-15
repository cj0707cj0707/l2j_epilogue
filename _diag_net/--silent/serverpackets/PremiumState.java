package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PremiumState extends ServerPacket {
   private final int _objectId;
   private final int _state;

   public PremiumState(int objectId, int state) {
      this._objectId = objectId;
      this._state = state;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BR_PREMIUM_STATE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeByte(this._state);
   }
}
