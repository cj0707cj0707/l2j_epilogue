package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPutEnchantSupportItemResult extends ServerPacket {
   private final int _result;

   public ExPutEnchantSupportItemResult(int result) {
      this._result = result;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PUT_ENCHANT_SUPPORT_ITEM_RESULT.writeId(this, buffer);
      buffer.writeInt(this._result);
   }
}
