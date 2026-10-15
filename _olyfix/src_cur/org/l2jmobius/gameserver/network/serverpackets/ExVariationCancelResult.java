package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExVariationCancelResult extends ServerPacket {
   private final int _closeWindow = 1;
   private final int _result;

   public ExVariationCancelResult(int result) {
      this._result = result;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_VARIATION_CANCEL_RESULT.writeId(this, buffer);
      buffer.writeInt(this._closeWindow);
      buffer.writeInt(this._result);
   }
}
