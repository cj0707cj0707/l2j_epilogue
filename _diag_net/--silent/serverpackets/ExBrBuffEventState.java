package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExBrBuffEventState extends ServerPacket {
   private final int _type;
   private final int _value;
   private final int _state;
   private final int _endtime;

   public ExBrBuffEventState(int type, int value, int state, int endtime) {
      this._type = type;
      this._value = value;
      this._state = state;
      this._endtime = endtime;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BR_BUFF_EVENT_STATE.writeId(this, buffer);
      buffer.writeInt(this._type);
      buffer.writeInt(this._value);
      buffer.writeInt(this._state);
      buffer.writeInt(this._endtime);
   }
}
