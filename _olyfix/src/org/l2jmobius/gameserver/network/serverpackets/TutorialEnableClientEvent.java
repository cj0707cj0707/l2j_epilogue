package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class TutorialEnableClientEvent extends ServerPacket {
   private int _eventId = 0;

   public TutorialEnableClientEvent(int event) {
      this._eventId = event;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TUTORIAL_ENABLE_CLIENT_EVENT.writeId(this, buffer);
      buffer.writeInt(this._eventId);
   }
}
