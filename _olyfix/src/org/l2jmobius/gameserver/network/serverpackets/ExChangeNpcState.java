package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExChangeNpcState extends ServerPacket {
   private final int _objId;
   private final int _state;

   public ExChangeNpcState(int objId, int state) {
      this._objId = objId;
      this._state = state;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BR_BROADCAST_EVENT_STATE.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._state);
   }
}
