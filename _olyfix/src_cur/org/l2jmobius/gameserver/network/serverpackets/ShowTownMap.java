package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ShowTownMap extends ServerPacket {
   private final String _texture;
   private final int _x;
   private final int _y;

   public ShowTownMap(String texture, int x, int y) {
      this._texture = texture;
      this._x = x;
      this._y = y;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SHOW_TOWN_MAP.writeId(this, buffer);
      buffer.writeString(this._texture);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
   }
}
