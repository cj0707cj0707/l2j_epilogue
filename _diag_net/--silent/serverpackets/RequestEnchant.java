package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class RequestEnchant extends ServerPacket {
   private final int _result;

   public RequestEnchant(int result) {
      this._result = result;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_PUT_ENCHANT_TARGET_ITEM_RESULT.writeId(this, buffer);
      buffer.writeInt(this._result);
   }
}
