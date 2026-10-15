package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBrBroadcastEventState extends ServerPacket {
   public static final int APRIL_FOOLS = 20090401;
   public static final int EVAS_INFERNO = 20090801;
   public static final int HALLOWEEN_EVENT = 20091031;
   public static final int RAISING_RUDOLPH = 20091225;
   public static final int LOVERS_JUBILEE = 20100214;
   private final int _eventId;
   private final int _eventState;
   private int _param0;
   private int _param1;
   private int _param2;
   private int _param3;
   private int _param4;
   private String _param5;
   private String _param6;

   public ExBrBroadcastEventState(int eventId, int eventState) {
      this._eventId = eventId;
      this._eventState = eventState;
   }

   public ExBrBroadcastEventState(int eventId, int eventState, int param0, int param1, int param2, int param3, int param4, String param5, String param6) {
      this._eventId = eventId;
      this._eventState = eventState;
      this._param0 = param0;
      this._param1 = param1;
      this._param2 = param2;
      this._param3 = param3;
      this._param4 = param4;
      this._param5 = param5;
      this._param6 = param6;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BR_BROADCAST_EVENT_STATE.writeId(this, buffer);
      buffer.writeInt(this._eventId);
      buffer.writeInt(this._eventState);
      buffer.writeInt(this._param0);
      buffer.writeInt(this._param1);
      buffer.writeInt(this._param2);
      buffer.writeInt(this._param3);
      buffer.writeInt(this._param4);
      buffer.writeString(this._param5);
      buffer.writeString(this._param6);
   }
}
