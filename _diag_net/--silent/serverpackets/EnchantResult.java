package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class EnchantResult extends ServerPacket {
   private final int _result;
   private final int _crystal;
   private final int _count;

   public EnchantResult(int result, int crystal, int count) {
      this._result = result;
      this._crystal = crystal;
      this._count = count;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ENCHANT_RESULT.writeId(this, buffer);
      buffer.writeInt(this._result);
      buffer.writeInt(this._crystal);
      buffer.writeLong(this._count);
   }
}
