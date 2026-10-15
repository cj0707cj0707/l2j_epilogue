package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SpecialString extends ServerPacket {
   private final int _strId;
   private final int _fontSize;
   private final int _x;
   private final int _y;
   private final int _color;
   private final boolean _isDraw;
   private final String _text;

   public SpecialString(int strId, boolean isDraw, int fontSize, int x, int y, int color, String text) {
      this._strId = strId;
      this._isDraw = isDraw;
      this._fontSize = fontSize;
      this._x = x;
      this._y = y;
      this._color = color;
      this._text = text;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SERVER_CLOSE_SOCKET.writeId(this, buffer);
      buffer.writeByte(this._strId);
      buffer.writeByte(this._isDraw);
      buffer.writeByte(this._fontSize);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._color);
      buffer.writeString(this._text);
   }
}
