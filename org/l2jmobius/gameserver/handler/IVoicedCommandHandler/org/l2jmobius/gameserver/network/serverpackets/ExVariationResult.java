package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExVariationResult extends ServerPacket {
   private final int _stat12;
   private final int _stat34;
   private final int _unk3;

   public ExVariationResult(int unk1, int unk2, int unk3) {
      this._stat12 = unk1;
      this._stat34 = unk2;
      this._unk3 = unk3;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_VARIATION_RESULT.writeId(this, buffer);
      buffer.writeInt(this._stat12);
      buffer.writeInt(this._stat34);
      buffer.writeInt(this._unk3);
   }
}
