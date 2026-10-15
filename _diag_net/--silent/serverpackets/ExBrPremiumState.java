package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExBrPremiumState extends ServerPacket {
   private final int _objId;
   private final int _state;

   public ExBrPremiumState(int id, int state) {
      this._objId = id;
      this._state = state;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BR_PREMIUM_STATE.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeByte(this._state);
   }
}
