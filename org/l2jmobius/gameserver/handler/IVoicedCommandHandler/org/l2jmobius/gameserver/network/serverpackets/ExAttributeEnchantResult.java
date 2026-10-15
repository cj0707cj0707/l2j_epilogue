package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExAttributeEnchantResult extends ServerPacket {
   private final int _result;

   public ExAttributeEnchantResult(int result) {
      this._result = result;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_ATTRIBUTE_ENCHANT_RESULT.writeId(this, buffer);
      buffer.writeInt(this._result);
   }
}
