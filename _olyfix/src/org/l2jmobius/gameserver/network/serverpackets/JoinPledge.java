package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class JoinPledge extends ServerPacket {
   private final int _pledgeId;

   public JoinPledge(int pledgeId) {
      this._pledgeId = pledgeId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.JOIN_PLEDGE.writeId(this, buffer);
      buffer.writeInt(this._pledgeId);
   }
}
