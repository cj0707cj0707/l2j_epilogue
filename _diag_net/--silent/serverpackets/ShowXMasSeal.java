package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ShowXMasSeal extends ServerPacket {
   private final int _item;

   public ShowXMasSeal(int item) {
      this._item = item;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHOW_XMAS_SEAL.writeId(this, buffer);
      buffer.writeInt(this._item);
   }
}
