package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExSendUIEvent extends ServerPacket {
   private final int _objectId;
   private final boolean _type;
   private final boolean _countUp;
   private final int _startTime;
   private final int _endTime;
   private final String _text;

   public ExSendUIEvent(Player player, boolean hide, boolean countUp, int startTime, int endTime, String text) {
      this._objectId = player.getObjectId();
      this._type = hide;
      this._countUp = countUp;
      this._startTime = startTime;
      this._endTime = endTime;
      this._text = text;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SEND_UI_EVENT.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._type);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeString(this._countUp ? "1" : "0");
      buffer.writeString(String.valueOf(this._startTime / 60));
      buffer.writeString(String.valueOf(this._startTime % 60));
      buffer.writeString(this._text);
      buffer.writeString(String.valueOf(this._endTime / 60));
      buffer.writeString(String.valueOf(this._endTime % 60));
   }
}
